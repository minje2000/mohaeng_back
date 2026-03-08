package org.poolpool.mohaeng.event.wishlist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistCreateRequestDto;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistItemDto;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistToggleRequestDto;
import org.poolpool.mohaeng.event.wishlist.exception.WishlistAlreadyExistsException;
import org.poolpool.mohaeng.event.wishlist.exception.WishlistNotFoundOrForbiddenException;
import org.poolpool.mohaeng.event.wishlist.service.EventWishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

@WithMockUser(username = "1", roles = "USER") //  Authentication.getName() == "1"
@WebMvcTest(controllers = WishlistController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(WishlistControllerWebMvcTest.TestSecurityConfig.class)
class WishlistControllerWebMvcTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockBean EventWishlistService wishlistService;

  private ResultHandler printResponseBodyPretty() {
    return (MvcResult result) -> {
      String body = result.getResponse().getContentAsString();

      System.out.println("\n===== RESPONSE BODY =====");
      if (body == null || body.isBlank()) {
        System.out.println("(empty body)");
      } else {
        try {
          System.out.println(
              objectMapper.writerWithDefaultPrettyPrinter()
                  .writeValueAsString(objectMapper.readTree(body))
          );
        } catch (Exception e) {
          System.out.println(body);
        }
      }
      System.out.println("=========================\n");
    };
  }

  @Test
  void create_성공_200() throws Exception {
    WishlistCreateRequestDto req = new WishlistCreateRequestDto();
    req.setEventId(10L);

    when(wishlistService.add(eq(1L), any(WishlistCreateRequestDto.class))).thenReturn(123L);

    mockMvc.perform(post("/api/users/1/wishlist")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andDo(printResponseBodyPretty())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(123));

    verify(wishlistService).add(eq(1L), any(WishlistCreateRequestDto.class));
  }

  @Test
  void create_중복_409() throws Exception {
    WishlistCreateRequestDto req = new WishlistCreateRequestDto();
    req.setEventId(10L);

    when(wishlistService.add(eq(1L), any(WishlistCreateRequestDto.class)))
        .thenThrow(new WishlistAlreadyExistsException(1L, 10L));

    mockMvc.perform(post("/api/users/1/wishlist")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andDo(printResponseBodyPretty())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void list_성공_200() throws Exception {
    PageResponse<WishlistItemDto> page = new PageResponse<>(List.of(), 1, 10, 0L, 0);
    when(wishlistService.getList(eq(1L), any(Pageable.class))).thenReturn(page);

    mockMvc.perform(get("/api/users/1/wishlist")
            .param("page", "1")
            .param("size", "10"))
        .andDo(printResponseBodyPretty())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.size").value(10));
  }

  @Test
  void toggle_없음또는권한없음_404() throws Exception {
    WishlistToggleRequestDto req = new WishlistToggleRequestDto();
    req.setEnabled(false);

    when(wishlistService.toggleNotification(eq(1L), eq(99L), any(WishlistToggleRequestDto.class)))
        .thenThrow(new WishlistNotFoundOrForbiddenException(99L));

    mockMvc.perform(put("/api/wishlist/99/notification")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andDo(printResponseBodyPretty())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void delete_없음또는권한없음_404() throws Exception {
    doThrow(new WishlistNotFoundOrForbiddenException(99L))
        .when(wishlistService).remove(1L, 99L);

    mockMvc.perform(delete("/api/wishlist/99")
            .with(csrf()))
        .andDo(printResponseBodyPretty())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false));

    verify(wishlistService).remove(1L, 99L);
  }

  @TestConfiguration
  static class TestSecurityConfig {
    @Bean
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
      return http
          .authorizeHttpRequests(auth -> auth
              .requestMatchers("/api/**").hasRole("USER")
              .anyRequest().permitAll()
          )
          .httpBasic(Customizer.withDefaults())
          .build();
    }
  }
}