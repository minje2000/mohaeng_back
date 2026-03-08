package org.poolpool.mohaeng.event.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.common.api.GlobalExceptionHandler;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.review.dto.EventReviewTabItemDto;
import org.poolpool.mohaeng.event.review.dto.MyPageReviewItemDto;
import org.poolpool.mohaeng.event.review.dto.ReviewCreateRequestDto;
import org.poolpool.mohaeng.event.review.dto.ReviewEditRequestDto;
import org.poolpool.mohaeng.event.review.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;

@WebMvcTest(controllers = ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReviewControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ReviewService reviewService;

    private static final String BASE = "/api";
    private static final String USER_HEADER = "userId";

    private static ResultHandler printUtf8(String label) {
        return result -> {
            String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            System.out.println("\n===== " + label + " RESPONSE (UTF-8) =====");
            System.out.println(body);
            System.out.println("========================================\n");
        };
    }

    @Test
    void myList_200() throws Exception {
        var data = new PageResponse<MyPageReviewItemDto>(List.of(), 0, 10, 0L, 0);
        given(reviewService.selectMyList(eq(1L), any(Pageable.class))).willReturn(data);

        mockMvc.perform(get(BASE + "/users/{userId}/reviews", 1L)
                        .header(USER_HEADER, "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(printUtf8("myList_200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(reviewService).should().selectMyList(eq(1L), any(Pageable.class));
    }

    //  path userId != header userId -> 403 (이제 500으로 안 바뀜)
    @Test
    void myList_userIdMismatch_403() throws Exception {
        mockMvc.perform(get(BASE + "/users/{userId}/reviews", 2L)
                        .header(USER_HEADER, "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(printUtf8("myList_userIdMismatch_403"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("요청 userId가 일치하지 않습니다."));

        then(reviewService).shouldHaveNoInteractions();
    }

    @Test
    void eventList_200() throws Exception {
        var data = new PageResponse<EventReviewTabItemDto>(List.of(), 0, 10, 0L, 0);
        given(reviewService.selectEventReviews(eq(1L), eq(10L), any(Pageable.class))).willReturn(data);

        mockMvc.perform(get(BASE + "/events/{eventId}/reviews", 10L)
                        .header(USER_HEADER, "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(printUtf8("eventList_200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(reviewService).should().selectEventReviews(eq(1L), eq(10L), any(Pageable.class));
    }

    @Test
    void myReview_200() throws Exception {
        given(reviewService.selectMyReviewForEvent(eq(1L), eq(10L))).willReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/events/{eventId}/reviews/my", 10L)
                        .header(USER_HEADER, "1"))
                .andDo(printUtf8("myReview_200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(reviewService).should().selectMyReviewForEvent(1L, 10L);
    }

    @Test
    void create_200() throws Exception {
        given(reviewService.create(eq(1L), any(ReviewCreateRequestDto.class))).willReturn(100L);

        String body = """
                {
                  "eventId": 10,
                  "ratingContent": 5,
                  "ratingProgress": 4,
                  "ratingMood": 5,
                  "content": "좋아요"
                }
                """;

        mockMvc.perform(post(BASE + "/reviews")
                        .header(USER_HEADER, "1")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(printUtf8("create_200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(reviewService).should().create(eq(1L), any(ReviewCreateRequestDto.class));
    }

    @Test
    void update_200() throws Exception {
        given(reviewService.edit(eq(1L), eq(100L), any(ReviewEditRequestDto.class))).willReturn(true);

        String body = """
                {
                  "ratingContent": 4,
                  "ratingProgress": 3,
                  "ratingMood": 4,
                  "content": "수정했어요"
                }
                """;

        mockMvc.perform(put(BASE + "/reviews/{reviewId}", 100L)
                        .header(USER_HEADER, "1")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(printUtf8("update_200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(reviewService).should().edit(eq(1L), eq(100L), any(ReviewEditRequestDto.class));
    }

    @Test
    void delete_200() throws Exception {
        given(reviewService.delete(eq(1L), eq(100L))).willReturn(true);

        mockMvc.perform(delete(BASE + "/reviews/{reviewId}", 100L)
                        .header(USER_HEADER, "1"))
                .andDo(printUtf8("delete_200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(reviewService).should().delete(1L, 100L);
    }

    //  헤더 누락 테스트: "실제 존재하는" 엔드포인트로 수정
    @Test
    void header_missing_500() throws Exception {
        mockMvc.perform(get(BASE + "/events/{eventId}/reviews/my", 10L))
                .andDo(printUtf8("header_missing_500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("서버 오류"));
    }
}