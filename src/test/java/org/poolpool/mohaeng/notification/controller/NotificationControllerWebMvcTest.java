package org.poolpool.mohaeng.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.common.api.GlobalExceptionHandler;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.notification.dto.NotificationItemDto;
import org.poolpool.mohaeng.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NotificationControllerWebMvcTest {

    @Autowired MockMvc mockMvc;
    @MockBean NotificationService notificationService;

    private static ResultHandler printUtf8(String label) {
        return result -> {
            String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            System.out.println("\n===== " + label + " RESPONSE (UTF-8) =====");
            System.out.println(body);
            System.out.println("========================================\n");
        };
    }

    @Test
    void list_200() throws Exception {
        var item = NotificationItemDto.builder()
                .notificationId(1L)
                .notiTypeId(6L)
                .notiTypeName("REPORT_ACCEPT")
                .contents("테스트")
                .createdAt(LocalDateTime.now())
                .build();

        var data = new PageResponse<>(List.of(item), 0, 5, 1L, 1);
        given(notificationService.getList(eq(1L), any())).willReturn(data);

        mockMvc.perform(get("/api/notifications")
                        .header("userId", "1")
                        .param("page", "0")
                        .param("size", "5"))
                .andDo(printUtf8("list_200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 알림 목록 조회 성공"));

        then(notificationService).should().getList(eq(1L), any());
    }

    @Test
    void count_200() throws Exception {
        given(notificationService.count(1L)).willReturn(3L);

        mockMvc.perform(get("/api/notifications/count")
                        .header("userId", "1"))
                .andDo(printUtf8("count_200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(3));

        then(notificationService).should().count(1L);
    }

    @Test
    void read_200() throws Exception {
        mockMvc.perform(delete("/api/notifications/{notificationId}", 10L)
                        .header("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(printUtf8("read_200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 읽음 처리 성공"));

        then(notificationService).should().read(1L, 10L);
    }

    @Test
    void readAll_200() throws Exception {
        mockMvc.perform(delete("/api/notifications")
                        .header("userId", "1"))
                .andDo(printUtf8("readAll_200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("전체 알림 읽음 처리 성공"));

        then(notificationService).should().readAll(1L);
    }
}