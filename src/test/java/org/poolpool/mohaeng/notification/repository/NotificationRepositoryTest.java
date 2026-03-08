package org.poolpool.mohaeng.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.notification.entity.NotificationEntity;
import org.poolpool.mohaeng.notification.entity.NotificationTypeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/mohaeng_test?serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true",
    "spring.datasource.username=poolpool",
    "spring.datasource.password=...", // 비번
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.jpa.show-sql=true"
})
@EntityScan(basePackages = {"org.poolpool.mohaeng"})
@EnableJpaRepositories(basePackages = {"org.poolpool.mohaeng"})
class NotificationRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired NotificationRepository notificationRepository;
    @Autowired NotificationTypeRepository notificationTypeRepository;

    private static final ObjectMapper OM = new ObjectMapper().findAndRegisterModules();
    private static final ObjectWriter PRETTY = OM.writerWithDefaultPrettyPrinter();

    private static void printApiJson(String label, boolean success, String message, Object data) {
        try {
            var body = new LinkedHashMap<String, Object>();
            body.put("success", success);
            body.put("message", message);
            body.put("data", data);
            body.put("timestamp", LocalDateTime.now().toString());

            PrintStream ps = new PrintStream(System.out, true, StandardCharsets.UTF_8);
            ps.println("\n===== " + label + " JSON (UTF-8) =====");
            ps.println(PRETTY.writeValueAsString(body));
            ps.println("====================================\n");
        } catch (Exception e) {
            System.out.println(label + " (JSON 출력 실패): " + e.getMessage());
        }
    }

    @Test
    void list_count_delete_readAll_works() {
        // 알림 타입(고정 ID) 준비
        notificationTypeRepository.save(
            NotificationTypeEntity.builder()
                .notiTypeId(1L)
                .notiTypeName("EVENT_DAY_BEFORE")
                .notiTypeContents("관심 행사 [TITLE] 전날")
                .build()
        );

        NotificationEntity n1 = notificationRepository.save(NotificationEntity.builder()
                .userId(1L)
                .notiTypeId(1L)
                .eventId(10L)
                .build());

        NotificationEntity n2 = notificationRepository.save(NotificationEntity.builder()
                .userId(1L)
                .notiTypeId(1L)
                .eventId(11L)
                .build());

        em.flush();

        long cnt1 = notificationRepository.countByUserId(1L);
        printApiJson("repo_count_1", true, "알림 개수 조회 성공", new LinkedHashMap<String, Object>() {{
            put("userId", 1L);
            put("count", cnt1);
        }});
        assertThat(cnt1).isEqualTo(2);

        var page = notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));
        printApiJson("repo_list", true, "내 알림 목록 조회 성공", new LinkedHashMap<String, Object>() {{
            put("userId", 1L);
            put("size", page.getContent().size());
            put("notificationIds", page.getContent().stream().map(NotificationEntity::getNotificationId).toList());
        }});
        assertThat(page.getContent()).hasSize(2);

        int deleted = notificationRepository.deleteByNotificationIdAndUserId(n1.getNotificationId(), 1L);
        long cnt2 = notificationRepository.countByUserId(1L);
        printApiJson("repo_read_delete_one", true, "알림 읽음(삭제) 처리", new LinkedHashMap<String, Object>() {{
            put("deletedRows", deleted);
            put("afterCount", cnt2);
        }});
        assertThat(deleted).isEqualTo(1);
        assertThat(cnt2).isEqualTo(1);

        int deletedAll = notificationRepository.deleteByUserId(1L);
        long cnt3 = notificationRepository.countByUserId(1L);
        printApiJson("repo_readAll_delete_all", true, "전체 알림 읽음(삭제) 처리", new LinkedHashMap<String, Object>() {{
            put("deletedRows", deletedAll);
            put("afterCount", cnt3);
        }});
        assertThat(deletedAll).isEqualTo(1);
        assertThat(cnt3).isZero();
    }
}