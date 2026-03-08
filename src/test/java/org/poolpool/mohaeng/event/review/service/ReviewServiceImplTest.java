package org.poolpool.mohaeng.event.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.review.dto.ReviewCreateRequestDto;
import org.poolpool.mohaeng.event.review.dto.ReviewEditRequestDto;
import org.poolpool.mohaeng.event.review.repository.ReviewRepository;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.type.SignupType;
import org.poolpool.mohaeng.user.type.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

// @Rollback(false)
@ActiveProfiles("test")
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/mohaeng_test?serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true",
    "spring.datasource.username=poolpool",
    "spring.datasource.password=...", 
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.jpa.show-sql=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages = {"org.poolpool.mohaeng"})
@EnableJpaRepositories(basePackages = {"org.poolpool.mohaeng"})
@Import(ReviewServiceImpl.class) // 서비스 빈을 테스트에서 직접 등록
class ReviewServiceImplTest {

    @Autowired ReviewService reviewService;
    @Autowired ReviewRepository reviewRepository;
    @Autowired TestEntityManager em;

    // JSON 출력(컨트롤러 ApiResponse 느낌)
    private static final ObjectMapper OM = new ObjectMapper().findAndRegisterModules();
    private static final ObjectWriter PRETTY = OM.writerWithDefaultPrettyPrinter();

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

    // UserEntity 필수값(email, name, userType, signupType) 채워서 저장
    private UserEntity persistUser(String name) {
        UserEntity u = UserEntity.builder()
            .email(name + "_" + UUID.randomUUID() + "@test.com") // UNIQUE + NOT NULL
            .name(name)                                         // NOT NULL
            .userType(UserType.PERSONAL)                        // NOT NULL
            .signupType(SignupType.BASIC)                       // NOT NULL
            .build();

        em.persist(u);
        em.flush();
        return u;
    }

    // EventEntity 필수값(title, hasBooth, hasFacility, views) 채워서 저장
    private EventEntity persistEvent(String title) {
        EventEntity ev = new EventEntity();
        ev.setTitle(title);          // NOT NULL
        ev.setHasBooth(false);       // NOT NULL
        ev.setHasFacility(false);    // NOT NULL
        ev.setViews(0);              // NOT NULL
        em.persist(ev);
        em.flush();
        return ev;
    }

    @Test
    void 리뷰작성_성공() {
        UserEntity u = persistUser("tester");
        EventEntity ev = persistEvent("event");

        ReviewCreateRequestDto req = new ReviewCreateRequestDto();
        req.setEventId(ev.getEventId());
        req.setRatingContent(5);
        req.setRatingProgress(4);
        req.setRatingMood(5);
        req.setContent("좋아요");

        long reviewId = reviewService.create(u.getUserId(), req);

        var data = new LinkedHashMap<String, Object>();
        data.put("userId", u.getUserId());
        data.put("eventId", ev.getEventId());
        data.put("reviewId", reviewId);

        printApiJson("리뷰작성_성공", true, "리뷰 작성 성공", data);

        assertThat(reviewId).isPositive();
        assertThat(reviewRepository.existsByUser_UserIdAndEvent_EventId(u.getUserId(), ev.getEventId())).isTrue();
    }

    @Test
    void 같은이벤트_중복작성_막힘() {
        UserEntity u = persistUser("tester");
        EventEntity ev = persistEvent("event");

        ReviewCreateRequestDto req = new ReviewCreateRequestDto();
        req.setEventId(ev.getEventId());
        req.setRatingContent(5);
        req.setRatingProgress(4);
        req.setRatingMood(5);
        req.setContent("첫 리뷰");

        long firstId = reviewService.create(u.getUserId(), req);

        var firstData = new LinkedHashMap<String, Object>();
        firstData.put("userId", u.getUserId());
        firstData.put("eventId", ev.getEventId());
        firstData.put("reviewId", firstId);
        printApiJson("같은이벤트_첫작성", true, "첫 리뷰 작성 성공", firstData);

        assertThatThrownBy(() -> reviewService.create(u.getUserId(), req))
            .isInstanceOf(IllegalStateException.class);

        var dupData = new LinkedHashMap<String, Object>();
        dupData.put("userId", u.getUserId());
        dupData.put("eventId", ev.getEventId());
        printApiJson("같은이벤트_중복작성", false, "같은 이벤트에 중복 작성은 불가", dupData);
    }

    @Test
    void 다른사람리뷰_수정삭제_불가() {
        UserEntity writer = persistUser("writer");
        UserEntity other = persistUser("other");
        EventEntity ev = persistEvent("event");

        ReviewCreateRequestDto createReq = new ReviewCreateRequestDto();
        createReq.setEventId(ev.getEventId());
        createReq.setRatingContent(5);
        createReq.setRatingProgress(5);
        createReq.setRatingMood(5);
        createReq.setContent("작성자 리뷰");

        long reviewId = reviewService.create(writer.getUserId(), createReq);

        var created = new LinkedHashMap<String, Object>();
        created.put("writerId", writer.getUserId());
        created.put("eventId", ev.getEventId());
        created.put("reviewId", reviewId);
        printApiJson("작성자_리뷰생성", true, "작성자 리뷰 생성 성공", created);

        ReviewEditRequestDto editReq = new ReviewEditRequestDto();
        editReq.setRatingContent(1);
        editReq.setRatingProgress(1);
        editReq.setRatingMood(1);
        editReq.setContent("남이 수정 시도");

        // 타인이 수정 -> 예외
        assertThatThrownBy(() -> reviewService.edit(other.getUserId(), reviewId, editReq))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("본인 리뷰");

        var editFail = new LinkedHashMap<String, Object>();
        editFail.put("attemptUserId", other.getUserId());
        editFail.put("reviewId", reviewId);
        printApiJson("타인_수정시도", false, "본인 리뷰만 수정할 수 있습니다.", editFail);

        // 타인이 삭제 -> false (서비스가 예외를 던지는 구현이어도 테스트가 깨지지 않게 흡수)
        boolean otherDeleteResult;
        try {
            otherDeleteResult = reviewService.delete(other.getUserId(), reviewId);
        } catch (IllegalArgumentException e) {
            otherDeleteResult = false;
        }

        var delFail = new LinkedHashMap<String, Object>();
        delFail.put("attemptUserId", other.getUserId());
        delFail.put("reviewId", reviewId);
        delFail.put("deleted", otherDeleteResult);
        printApiJson("타인_삭제시도", false, "본인 리뷰만 삭제할 수 있습니다.", delFail);

        assertThat(otherDeleteResult).isFalse();

        // 작성자가 삭제 -> true
        boolean writerDeleteResult = reviewService.delete(writer.getUserId(), reviewId);

        var delOk = new LinkedHashMap<String, Object>();
        delOk.put("writerId", writer.getUserId());
        delOk.put("reviewId", reviewId);
        delOk.put("deleted", writerDeleteResult);
        printApiJson("작성자_삭제", true, "리뷰 삭제 성공", delOk);

        assertThat(writerDeleteResult).isTrue();
    }
}