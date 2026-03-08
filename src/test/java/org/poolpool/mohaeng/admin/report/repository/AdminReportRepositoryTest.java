package org.poolpool.mohaeng.admin.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.admin.report.entity.AdminReportFEntity;
import org.poolpool.mohaeng.admin.report.type.ReportResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
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
class AdminReportRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired AdminReportRepository reportRepository;

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

    private AdminReportFEntity persistReport(long eventId, long reporterId, String category, String result) {
        AdminReportFEntity r = AdminReportFEntity.builder()
            .eventId(eventId)
            .reporterId(reporterId)
            .reasonCategory(category)
            .reasonDetailText("상세")
            .reportResult(result) // null이면 PrePersist가 PENDING
            .build();
        em.persist(r);
        em.flush();
        return r;
    }

    @Test
    void existsByReporterIdAndEventId_true() {
        AdminReportFEntity r = persistReport(10L, 1L, "스팸", ReportResult.PENDING);

        boolean exists = reportRepository.existsByReporterIdAndEventId(1L, 10L);

        printApiJson("existsByReporterIdAndEventId",
            true, "중복 신고 여부 조회",
            new LinkedHashMap<String, Object>() {{
                put("reportId", r.getReportId());
                put("eventId", 10L);
                put("reporterId", 1L);
                put("exists", exists);
            }}
        );

        assertThat(exists).isTrue();
    }

    @Test
    void deleteByEventIdAndReportIdNot_deletes_all_other_reports() {
        long eventId = 10L;

        AdminReportFEntity keep = persistReport(eventId, 1L, "스팸", ReportResult.PENDING);
        AdminReportFEntity other1 = persistReport(eventId, 2L, "욕설", ReportResult.PENDING);
        AdminReportFEntity other2 = persistReport(eventId, 3L, "기타", ReportResult.REJECTED);

        long deleted = reportRepository.deleteByEventIdAndReportIdNot(eventId, keep.getReportId());
        em.flush();

        long remaining = reportRepository.findAll().stream()
            .filter(x -> x.getEventId().equals(eventId))
            .count();

        printApiJson("deleteByEventIdAndReportIdNot",
            true, "승인 시 같은 이벤트의 다른 신고 삭제",
            new LinkedHashMap<String, Object>() {{
                put("eventId", eventId);
                put("keepReportId", keep.getReportId());
                put("deletedCount", deleted);
                put("remainingForEvent", remaining);
                put("deletedReportIds", new long[]{other1.getReportId(), other2.getReportId()});
            }}
        );

        assertThat(deleted).isEqualTo(2);
        assertThat(remaining).isEqualTo(1);
        assertThat(reportRepository.findById(keep.getReportId())).isPresent();
    }
}