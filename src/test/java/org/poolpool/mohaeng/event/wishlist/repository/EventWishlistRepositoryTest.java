package org.poolpool.mohaeng.event.wishlist.repository;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.common.api.ApiResponse;
import org.poolpool.mohaeng.event.wishlist.entity.EventWishlistEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

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
class EventWishlistRepositoryTest {

  @Autowired EventWishlistRepository wishlistRepository;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  {
    objectMapper.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
  }

  private void printAsJson(String label, Object value) {
    try {
      System.out.println("\n===== " + label + " =====");
      System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value));
      System.out.println("========================\n");
    } catch (Exception e) {
      System.out.println("\n===== " + label + " (toString) =====");
      System.out.println(String.valueOf(value));
      System.out.println("==================================\n");
    }
  }

  @Test
  void existsByUserIdAndEventId_정상동작() {
    EventWishlistEntity w = new EventWishlistEntity();
    w.setUserId(1L);
    w.setEventId(10L);
    w.setNotificationEnabled(true);

    EventWishlistEntity saved = wishlistRepository.saveAndFlush(w);
    printAsJson("저장 결과(한글 포함)", ApiResponse.ok("관심행사 저장 완료", saved));

    boolean exists10 = wishlistRepository.existsByUserIdAndEventId(1L, 10L);
    boolean exists11 = wishlistRepository.existsByUserIdAndEventId(1L, 11L);

    printAsJson("exists 결과(한글 포함)",
        ApiResponse.ok("existsByUserIdAndEventId 조회 결과", Map.of("eventId=10", exists10, "eventId=11", exists11)));

    assertThat(exists10).isTrue();
    assertThat(exists11).isFalse();
  }

  @Test
  void findByUserIdOrderByCreatedAtDesc_최신순() {
    EventWishlistEntity w1 = new EventWishlistEntity();
    w1.setUserId(1L);
    w1.setEventId(10L);
    w1.setNotificationEnabled(true);
    wishlistRepository.saveAndFlush(w1);

    EventWishlistEntity w2 = new EventWishlistEntity();
    w2.setUserId(1L);
    w2.setEventId(11L);
    w2.setNotificationEnabled(true);
    wishlistRepository.saveAndFlush(w2);

    var page = wishlistRepository.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));

    printAsJson("최신순 조회(한글 포함)", ApiResponse.ok("관심행사 최신순 조회 결과", page.getContent()));

    assertThat(page.getContent()).hasSize(2);
    assertThat(page.getContent().get(0).getEventId()).isEqualTo(11L);
    assertThat(page.getContent().get(1).getEventId()).isEqualTo(10L);
  }

  @Test
  void deleteByWishIdAndUserId_본인것만삭제() {
    EventWishlistEntity w = new EventWishlistEntity();
    w.setUserId(1L);
    w.setEventId(10L);
    w.setNotificationEnabled(true);
    EventWishlistEntity saved = wishlistRepository.saveAndFlush(w);

    int deletedWrongUser = wishlistRepository.deleteByWishIdAndUserId(saved.getWishId(), 2L);
    printAsJson("삭제 시도(다른 유저)", ApiResponse.ok("다른 유저 삭제 시도 결과", Map.of("deleted", deletedWrongUser)));

    int deleted = wishlistRepository.deleteByWishIdAndUserId(saved.getWishId(), 1L);
    printAsJson("삭제 결과(본인)", ApiResponse.ok("관심행사 삭제 완료", Map.of("deleted", deleted, "wishId", saved.getWishId())));

    assertThat(deletedWrongUser).isEqualTo(0);
    assertThat(deleted).isEqualTo(1);
    assertThat(wishlistRepository.existsById(saved.getWishId())).isFalse();
  }

  @Test
  void updateNotificationEnabledByWishIdAndUserId_토글() {
    EventWishlistEntity w = new EventWishlistEntity();
    w.setUserId(1L);
    w.setEventId(10L);
    w.setNotificationEnabled(true);
    EventWishlistEntity saved = wishlistRepository.saveAndFlush(w);

    int updatedWrongUser =
        wishlistRepository.updateNotificationEnabledByWishIdAndUserId(saved.getWishId(), 2L, false);
    printAsJson("토글 시도(다른 유저)", ApiResponse.ok("다른 유저 토글 시도 결과", Map.of("updated", updatedWrongUser)));

    int updated =
        wishlistRepository.updateNotificationEnabledByWishIdAndUserId(saved.getWishId(), 1L, false);

    var latest = wishlistRepository.findByWishIdAndUserId(saved.getWishId(), 1L).orElseThrow();

    printAsJson("토글 결과(본인)", ApiResponse.ok("알림 토글 완료", Map.of("updated", updated, "latest", latest)));

    assertThat(updatedWrongUser).isEqualTo(0);
    assertThat(updated).isEqualTo(1);
    assertThat(latest.isNotificationEnabled()).isFalse();
  }

  @Test
  void uniqueConstraint_같은유저_같은이벤트_중복저장_예외() {
    EventWishlistEntity w1 = new EventWishlistEntity();
    w1.setUserId(1L);
    w1.setEventId(10L);
    w1.setNotificationEnabled(true);
    EventWishlistEntity saved1 = wishlistRepository.saveAndFlush(w1);

    printAsJson("첫 저장(한글 포함)", ApiResponse.ok("첫 관심행사 저장 완료", saved1));

    EventWishlistEntity w2 = new EventWishlistEntity();
    w2.setUserId(1L);
    w2.setEventId(10L);
    w2.setNotificationEnabled(true);

    assertThatThrownBy(() -> wishlistRepository.saveAndFlush(w2))
        .isInstanceOf(DataIntegrityViolationException.class);

    printAsJson("중복 저장(한글 포함)", ApiResponse.fail("중복 저장으로 예외 발생(DataIntegrityViolationException)", null));
  }
}