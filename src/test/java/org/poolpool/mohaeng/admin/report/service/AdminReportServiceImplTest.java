package org.poolpool.mohaeng.admin.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.admin.report.dto.AdminReportCreateRequestDto;
import org.poolpool.mohaeng.admin.report.repository.AdminReportRepository;
import org.poolpool.mohaeng.admin.report.type.ReportResult;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
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
    "spring.datasource.password=...", //  너 비번
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.jpa.show-sql=true"
})
@EntityScan(basePackages = {"org.poolpool.mohaeng"})
@EnableJpaRepositories(basePackages = {"org.poolpool.mohaeng"})
@Import(AdminReportServiceImpl.class)
class AdminReportServiceImplTest {

    @Autowired AdminReportService reportService;
    @Autowired AdminReportRepository reportRepository;
    @Autowired EventRepository eventRepository;
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

            String json = PRETTY.writeValueAsString(body);

            PrintStream ps = new PrintStream(System.out, true, StandardCharsets.UTF_8);
            ps.println("\n===== " + label + " JSON (UTF-8) =====");
            ps.println(json);
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
    void create_성공_and_getList_getDetail() {
        EventEntity ev = persistEvent("테스트 행사");

        AdminReportCreateRequestDto req = new AdminReportCreateRequestDto();
        req.setEventId(ev.getEventId());
        req.setReasonCategory("스팸/광고");
        req.setReasonDetailText("상세 내용");

        long reportId = reportService.create(1L, req);

        PageResponse<?> list = reportService.getList(PageRequest.of(0, 10));
        var detail = reportService.getDetail(reportId);

        printApiJson("create_성공",
            true, "신고 생성/목록/상세 확인",
            new LinkedHashMap<String, Object>() {{
                put("eventId", ev.getEventId());
                put("createdReportId", reportId);
                put("listSize", list.content().size()); //  getContent() -> content()
                put("detailEventName", detail.getEventName());
                put("detailReportResult", detail.getReportResult());
            }}
        );

        assertThat(reportId).isPositive();
        assertThat(list.content()).isNotEmpty();
        assertThat(detail.getReportId()).isEqualTo(reportId);
        assertThat(detail.getReportResult()).isEqualTo(ReportResult.PENDING);
        assertThat(detail.getEventName()).isEqualTo("테스트 행사");
    }

    @Test
    void create_중복신고_막힘() {
        EventEntity ev = persistEvent("중복 테스트");

        AdminReportCreateRequestDto req = new AdminReportCreateRequestDto();
        req.setEventId(ev.getEventId());
        req.setReasonCategory("기타");
        req.setReasonDetailText("첫 신고");

        long firstId = reportService.create(1L, req);

        printApiJson("create_첫신고",
            true, "첫 신고 생성",
            new LinkedHashMap<String, Object>() {{
                put("eventId", ev.getEventId());
                put("reporterId", 1L);
                put("reportId", firstId);
            }}
        );

        assertThatThrownBy(() -> reportService.create(1L, req))
            .isInstanceOf(IllegalStateException.class);

        printApiJson("create_중복차단",
            false, "이미 해당 이벤트를 신고했습니다.",
            new LinkedHashMap<String, Object>() {{
                put("eventId", ev.getEventId());
                put("reporterId", 1L);
            }}
        );
    }

    @Test
    void approve_이벤트비활성화_and_다른신고전부삭제() {
        EventEntity ev = persistEvent("승인 테스트");

        long r1 = reportService.create(1L, createReq(ev.getEventId(), "스팸", "A"));
        long r2 = reportService.create(2L, createReq(ev.getEventId(), "욕설", "B"));
        long r3 = reportService.create(3L, createReq(ev.getEventId(), "기타", "C"));

        reportService.reject(r2);

        reportService.approve(r1);
        em.flush();
        em.clear();

        EventEntity afterEvent = eventRepository.findById(ev.getEventId()).orElseThrow();
        var reportsForEvent = reportRepository.findAll().stream()
            .filter(x -> x.getEventId().equals(ev.getEventId()))
            .toList();

        printApiJson("approve_검증",
            true, "승인 시 이벤트 비활성화 + 같은 이벤트 다른 신고 전부 삭제",
            new LinkedHashMap<String, Object>() {{
                put("eventId", ev.getEventId());
                put("eventStatus", afterEvent.getEventStatus());
                put("remainingReportsCount", reportsForEvent.size());
                put("remainingReportId", reportsForEvent.get(0).getReportId());
                put("remainingReportResult", reportsForEvent.get(0).getReportResult());
                put("deletedReportIds", new long[]{r2, r3});
            }}
        );

        assertThat(afterEvent.getEventStatus()).isEqualTo("DELETED");
        assertThat(reportsForEvent).hasSize(1);
        assertThat(reportsForEvent.get(0).getReportId()).isEqualTo(r1);
        assertThat(reportsForEvent.get(0).getReportResult()).isEqualTo(ReportResult.APPROVED);
    }

    private AdminReportCreateRequestDto createReq(Long eventId, String cat, String detail) {
        AdminReportCreateRequestDto req = new AdminReportCreateRequestDto();
        req.setEventId(eventId);
        req.setReasonCategory(cat);
        req.setReasonDetailText(detail);
        return req;
    }
}