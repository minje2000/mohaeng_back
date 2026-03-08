package org.poolpool.mohaeng.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.admin.report.entity.AdminReportFEntity;
import org.poolpool.mohaeng.admin.report.repository.AdminReportRepository;
import org.poolpool.mohaeng.admin.report.type.ReportResult;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.notification.entity.NotificationEntity;
import org.poolpool.mohaeng.notification.entity.NotificationTypeEntity;
import org.poolpool.mohaeng.notification.repository.NotificationRepository;
import org.poolpool.mohaeng.notification.repository.NotificationTypeRepository;
import org.poolpool.mohaeng.notification.type.NotiTypeId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
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
@Import(NotificationServiceImpl.class)
class NotificationServiceImplTest { // report 머지가 안 되어서 에러

    @Autowired NotificationService notificationService;

    @Autowired NotificationRepository notificationRepository;
    @Autowired NotificationTypeRepository notificationTypeRepository;

    @Autowired EventRepository eventRepository;
    @Autowired AdminReportRepository reportRepository;

    @Autowired TestEntityManager em;

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

    private EventEntity persistEvent(String title) {
        EventEntity ev = new EventEntity();
        ev.setTitle(title);
        ev.setHasBooth(false);
        ev.setHasFacility(false);
        ev.setViews(0);
        em.persist(ev);
        em.flush();
        return ev;
    }

    @Test
    void create_getList_template_replace_and_read_delete() {
        EventEntity ev = persistEvent("테스트행사");

        AdminReportFEntity report = reportRepository.save(
            AdminReportFEntity.builder()
                .eventId(ev.getEventId())
                .reporterId(1L)
                .reasonCategory("스팸/광고")
                .reasonDetailText("상세")
                .reportResult(ReportResult.PENDING)
                .build()
        );

        notificationTypeRepository.save(
            NotificationTypeEntity.builder()
                .notiTypeId(NotiTypeId.REPORT_ACCEPT)
                .notiTypeName("REPORT_ACCEPT")
                .notiTypeContents("[TITLE]에 대한 [REASON_CATEGORY] 신고는 승인되었습니다.")
                .build()
        );

        long userId = 99L;

        long notiId = notificationService.create(
            userId, NotiTypeId.REPORT_ACCEPT, ev.getEventId(), report.getReportId()
        );

        var page = notificationService.getList(userId, PageRequest.of(0, 10));
        String contents = page.content().get(0).getContents();

        printApiJson("service_create_and_list", true, "알림 생성 + 목록 조회 성공", new LinkedHashMap<String, Object>() {{
            put("userId", userId);
            put("notificationId", notiId);
            put("eventTitle", ev.getTitle());
            put("reasonCategory", report.getReasonCategory());
            put("contents", contents);
        }});

        assertThat(page.content()).hasSize(1);
        assertThat(contents).contains("테스트행사");
        assertThat(contents).contains("스팸/광고");

        long cnt = notificationService.count(userId);
        printApiJson("service_count", true, "알림 개수 조회 성공", new LinkedHashMap<String, Object>() {{
            put("userId", userId);
            put("count", cnt);
        }});
        assertThat(cnt).isEqualTo(1);

        notificationService.read(userId, notiId);
        long cntAfter = notificationService.count(userId);
        printApiJson("service_read", true, "알림 읽음(삭제) 처리 성공", new LinkedHashMap<String, Object>() {{
            put("userId", userId);
            put("deletedNotificationId", notiId);
            put("afterCount", cntAfter);
        }});
        assertThat(cntAfter).isZero();

        // 다른 유저로 삭제 시도 예외
        NotificationEntity again = notificationRepository.save(
            NotificationEntity.builder()
                .userId(userId)
                .notiTypeId(NotiTypeId.REPORT_ACCEPT)
                .eventId(ev.getEventId())
                .reportId(report.getReportId())
                .build()
        );
        em.flush();

        assertThatThrownBy(() -> notificationService.read(123L, again.getNotificationId()))
            .isInstanceOf(IllegalArgumentException.class);

        printApiJson("service_read_forbidden", false, "알림이 없거나 본인 알림이 아닙니다.", new LinkedHashMap<String, Object>() {{
            put("attemptUserId", 123L);
            put("notificationId", again.getNotificationId());
        }});
    }

    @Test
    void readAll_deletes_all() {
        notificationTypeRepository.save(
            NotificationTypeEntity.builder()
                .notiTypeId(NotiTypeId.EVENT_DAY_OF)
                .notiTypeName("EVENT_DAY_OF")
                .notiTypeContents("당일 [TITLE]")
                .build()
        );

        notificationRepository.save(NotificationEntity.builder().userId(1L).notiTypeId(NotiTypeId.EVENT_DAY_OF).eventId(10L).build());
        notificationRepository.save(NotificationEntity.builder().userId(1L).notiTypeId(NotiTypeId.EVENT_DAY_OF).eventId(11L).build());
        em.flush();

        long before = notificationService.count(1L);
        notificationService.readAll(1L);
        long after = notificationService.count(1L);

        printApiJson("service_readAll", true, "전체 알림 읽음(삭제) 처리 성공", new LinkedHashMap<String, Object>() {{
            put("userId", 1L);
            put("beforeCount", before);
            put("afterCount", after);
        }});

        assertThat(before).isEqualTo(2);
        assertThat(after).isZero();
    }
}