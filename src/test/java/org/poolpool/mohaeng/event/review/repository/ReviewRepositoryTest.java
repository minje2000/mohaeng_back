package org.poolpool.mohaeng.event.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.review.entity.ReviewEntity;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.type.SignupType;
import org.poolpool.mohaeng.user.type.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/mohaeng_test?serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true",
    "spring.datasource.username=poolpool",
    "spring.datasource.password=...",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.jpa.show-sql=true"
})
@Rollback(false) // DB 확인용 아니면 지우는 걸 추천(기본은 롤백)
@EntityScan(basePackages = {"org.poolpool.mohaeng"})
@EnableJpaRepositories(basePackages = {"org.poolpool.mohaeng.event.review.repository"})
class ReviewRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ReviewRepository reviewRepository;

    // JSON 출력용 (JavaTime 대응)
    private static final ObjectMapper OM = new ObjectMapper().findAndRegisterModules();
    private static final ObjectWriter PRETTY = OM.writerWithDefaultPrettyPrinter();

    // 컨트롤러 ApiResponse처럼 JSON 형태로 콘솔 출력 (UTF-8, 한글 OK)
    private static void printApiJson(String label, boolean success, String message, Object data) {
        try {
            var body = new LinkedHashMap<String, Object>();
            body.put("success", success);
            body.put("message", message);
            body.put("data", data);
            body.put("timestamp", LocalDateTime.now().toString());

            String json = PRETTY.writeValueAsString(body);

            PrintStream ps = new PrintStream(System.out, true, StandardCharsets.UTF_8);
            ps.println();
            ps.println("===== " + label + " JSON RESPONSE (UTF-8) =====");
            ps.println(json);
            ps.println("==============================================");
            ps.println();
        } catch (Exception e) {
            System.out.println(label + " (JSON 출력 실패): " + e.getMessage());
        }
    }

    private UserEntity persistUser(String name) {
        UserEntity u = UserEntity.builder()
                .email(name + "_" + UUID.randomUUID() + "@test.com") // UNIQUE
                .name(name)
                .userType(UserType.PERSONAL)      // NOT NULL
                .signupType(SignupType.BASIC)     // NOT NULL
                .build();
        em.persist(u);
        em.flush();
        return u;
    }

    private EventEntity persistEvent(String title) {
        EventEntity ev = new EventEntity();
        ev.setTitle(title);
        ev.setHasBooth(false);       // NOT NULL
        ev.setHasFacility(false);    // NOT NULL
        ev.setViews(0);              // NOT NULL
        em.persist(ev);
        em.flush();
        return ev;
    }

    private ReviewEntity persistReview(UserEntity user, EventEntity event, String content) {
        ReviewEntity r = new ReviewEntity();
        r.setUser(user);
        r.setEvent(event);
        r.setRatingContent(5);
        r.setRatingProgress(4);
        r.setRatingMood(5);
        r.setContent(content);
        em.persist(r);
        em.flush();
        return r;
    }

    @Test
    void existsByUser_UserIdAndEvent_EventId_true() {
        UserEntity u = persistUser("tester");
        EventEntity ev = persistEvent("event");
        persistReview(u, ev, "good");

        boolean exists = reviewRepository.existsByUser_UserIdAndEvent_EventId(
                u.getUserId(), ev.getEventId()
        );

        var data = new LinkedHashMap<String, Object>();
        data.put("userId", u.getUserId());
        data.put("eventId", ev.getEventId());
        data.put("exists", exists);

        printApiJson(
                "existsByUserIdAndEventId",
                true,
                "리뷰 존재 여부 조회 성공",
                data
        );

        assertThat(exists).isTrue();
    }

    @Test
    void findByEvent_EventIdAndUser_UserIdNotOrderByCreatedAtDesc_returns_other_users() {
        UserEntity me = persistUser("me");
        UserEntity other = persistUser("other");
        EventEntity ev = persistEvent("event");

        persistReview(me, ev, "mine");
        persistReview(other, ev, "others");

        var page = reviewRepository.findByEvent_EventIdAndUser_UserIdNotOrderByCreatedAtDesc(
                ev.getEventId(), me.getUserId(), PageRequest.of(0, 10)
        );

        List<Long> userIds = page.getContent().stream()
                .map(r -> r.getUser().getUserId())
                .toList();

        String userIdsCsv = userIds.stream().map(String::valueOf).collect(Collectors.joining(", "));

        var data = new LinkedHashMap<String, Object>();
        data.put("eventId", ev.getEventId());
        data.put("excludedUserId", me.getUserId());
        data.put("page", page.getNumber());
        data.put("size", page.getSize());
        data.put("totalElements", page.getTotalElements());
        data.put("numberOfElements", page.getNumberOfElements());
        data.put("userIds", userIds);
        data.put("userIdsCsv", userIdsCsv);

        printApiJson(
                "findByEventIdAndUserIdNot",
                true,
                "다른 유저 리뷰 목록 조회 성공",
                data
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUser().getUserId()).isEqualTo(other.getUserId());
    }
}