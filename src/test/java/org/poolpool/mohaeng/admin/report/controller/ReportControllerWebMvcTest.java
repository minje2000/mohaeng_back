package org.poolpool.mohaeng.admin.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.admin.report.dto.AdminReportCreateRequestDto;
import org.poolpool.mohaeng.admin.report.dto.AdminReportDetailDto;
import org.poolpool.mohaeng.admin.report.dto.AdminReportListItemDto;
import org.poolpool.mohaeng.admin.report.service.AdminReportService;
import org.poolpool.mohaeng.common.api.GlobalExceptionHandler;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.report.controller.EventReportController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;

@WebMvcTest(controllers = {AdminReportController.class, EventReportController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReportControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean AdminReportService reportService;

    private static ResultHandler printUtf8(String label) {
        return result -> {
            String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            System.out.println("\n===== " + label + " RESPONSE (UTF-8) =====");
            System.out.println(body);
            System.out.println("========================================\n");
        };
    }

    @Test
    void 사용자_신고등록_POST_api_reports_200() throws Exception {
        given(reportService.create(eq(1L), any(AdminReportCreateRequestDto.class))).willReturn(100L);

        String body = """
            {
              "eventId": 10,
              "reasonCategory": "스팸/광고",
              "reasonDetailText": "외부 링크 광고가 있어요"
            }
            """;

        mockMvc.perform(post("/api/reports")
                    .header("userId", "1")
                    .characterEncoding("UTF-8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andDo(printUtf8("사용자_신고등록"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("이벤트 신고 등록 성공"));

        then(reportService).should().create(eq(1L), any(AdminReportCreateRequestDto.class));
    }

    @Test
    void 관리자_신고목록_GET_api_admin_reports_200() throws Exception {
        var item = AdminReportListItemDto.builder()
            .reportId(1L)
            .eventId(10L)
            .eventName("테스트 행사")
            .reasonCategory("스팸/광고")
            .createdAt(LocalDateTime.now())
            .reportResult("PENDING")
            .build();

        var data = new PageResponse<AdminReportListItemDto>(List.of(item), 0, 10, 1L, 1);
        given(reportService.getList(any())).willReturn(data);

        mockMvc.perform(get("/api/admin/reports")
                    .param("page", "0")
                    .param("size", "10"))
                .andDo(printUtf8("관리자_신고목록"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("신고 목록 조회 성공"));

        then(reportService).should().getList(any());
    }

    @Test
    void 관리자_신고상세_GET_api_admin_reports_id_200() throws Exception {
        var detail = AdminReportDetailDto.builder()
            .reportId(1L)
            .eventId(10L)
            .eventName("테스트 행사")
            .reporterId(1L)
            .reasonCategory("스팸/광고")
            .reasonDetailText("상세")
            .createdAt(LocalDateTime.now())
            .reportResult("PENDING")
            .build();

        given(reportService.getDetail(1L)).willReturn(detail);

        mockMvc.perform(get("/api/admin/reports/{reportId}", 1L))
                .andDo(printUtf8("관리자_신고상세"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("신고 상세 조회 성공"));

        then(reportService).should().getDetail(1L);
    }

    @Test
    void 관리자_승인_PUT_api_admin_reports_id_approve_200() throws Exception {
        mockMvc.perform(put("/api/admin/reports/{reportId}/approve", 1L))
                .andDo(printUtf8("관리자_승인"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("신고 승인 처리 성공"));

        then(reportService).should().approve(1L);
    }

    @Test
    void 관리자_반려_PUT_api_admin_reports_id_reject_200() throws Exception {
        mockMvc.perform(put("/api/admin/reports/{reportId}/reject", 1L))
                .andDo(printUtf8("관리자_반려"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("신고 반려 처리 성공"));

        then(reportService).should().reject(1L);
    }
}