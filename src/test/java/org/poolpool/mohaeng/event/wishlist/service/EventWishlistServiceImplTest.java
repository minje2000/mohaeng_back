package org.poolpool.mohaeng.event.wishlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.common.api.ApiResponse;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistCreateRequestDto;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistItemDto;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistToggleRequestDto;
import org.poolpool.mohaeng.event.wishlist.exception.WishlistAlreadyExistsException;
import org.poolpool.mohaeng.event.wishlist.exception.WishlistNotFoundOrForbiddenException;
import org.poolpool.mohaeng.event.wishlist.repository.EventWishlistRepository;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.type.SignupType;
import org.poolpool.mohaeng.user.type.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
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
@Import(EventWishlistServiceImpl.class)
class EventWishlistServiceImplTest {

  @Autowired EventWishlistService wishlistService;
  @Autowired EventWishlistRepository wishlistRepository;
  @Autowired TestEntityManager em;

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

  private UserEntity persistUser(String name) {
    UserEntity u = UserEntity.builder()
        .name(name)
        .email(name + System.nanoTime() + "@test.com")
        .userType(UserType.PERSONAL)
        .signupType(SignupType.BASIC)
        .build();
    em.persist(u);
    em.flush();
    return u;
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
  void 관심등록_성공() {
    UserEntity u = persistUser("tester");
    EventEntity ev = persistEvent("event");

    WishlistCreateRequestDto req = new WishlistCreateRequestDto();
    req.setEventId(ev.getEventId());

    long wishId = wishlistService.add(u.getUserId(), req);

    printAsJson("create response(like controller)", ApiResponse.ok("관심행사 등록 완료", wishId));

    assertThat(wishId).isPositive();
    assertThat(wishlistRepository.existsByUserIdAndEventId(u.getUserId(), ev.getEventId())).isTrue();
  }

  @Test
  void 같은이벤트_중복등록_막힘() {
    UserEntity u = persistUser("tester");
    EventEntity ev = persistEvent("event");

    WishlistCreateRequestDto req = new WishlistCreateRequestDto();
    req.setEventId(ev.getEventId());

    long firstWishId = wishlistService.add(u.getUserId(), req);
    printAsJson("first create response(like controller)", ApiResponse.ok("관심행사 등록 완료", firstWishId));

    assertThatThrownBy(() -> wishlistService.add(u.getUserId(), req))
        .isInstanceOf(WishlistAlreadyExistsException.class);
  }

  @Test
  void 다른사람_삭제_토글_불가() {
    UserEntity writer = persistUser("writer");
    UserEntity other  = persistUser("other");
    EventEntity ev    = persistEvent("event");

    WishlistCreateRequestDto createReq = new WishlistCreateRequestDto();
    createReq.setEventId(ev.getEventId());

    long wishId = wishlistService.add(writer.getUserId(), createReq);
    printAsJson("create response(like controller)", ApiResponse.ok("관심행사 등록 완료", wishId));

    WishlistToggleRequestDto toggleReq = new WishlistToggleRequestDto();
    toggleReq.setEnabled(false);

    assertThatThrownBy(() -> wishlistService.toggleNotification(other.getUserId(), wishId, toggleReq))
        .isInstanceOf(WishlistNotFoundOrForbiddenException.class);

    assertThatThrownBy(() -> wishlistService.remove(other.getUserId(), wishId))
        .isInstanceOf(WishlistNotFoundOrForbiddenException.class);

    WishlistItemDto changed = wishlistService.toggleNotification(writer.getUserId(), wishId, toggleReq);
    printAsJson("toggle response(like controller)", ApiResponse.ok("알림 설정 변경 완료", changed));

    assertThat(wishlistRepository.findByWishIdAndUserId(wishId, writer.getUserId()).get().isNotificationEnabled())
        .isFalse();

    wishlistService.remove(writer.getUserId(), wishId);
    printAsJson("delete response(like controller)", ApiResponse.ok("관심행사 해제 완료", null));

    assertThat(wishlistRepository.existsById(wishId)).isFalse();
  }

  @Test
  void 목록조회_최신순_페이지응답() {
    UserEntity u = persistUser("tester");
    EventEntity ev1 = persistEvent("event1");
    EventEntity ev2 = persistEvent("event2");

    WishlistCreateRequestDto r1 = new WishlistCreateRequestDto();
    r1.setEventId(ev1.getEventId());
    wishlistService.add(u.getUserId(), r1);

    WishlistCreateRequestDto r2 = new WishlistCreateRequestDto();
    r2.setEventId(ev2.getEventId());
    wishlistService.add(u.getUserId(), r2);

    PageResponse<WishlistItemDto> page = wishlistService.getList(u.getUserId(), PageRequest.of(0, 10));
    printAsJson("list response(like controller)", ApiResponse.ok("관심행사 목록 조회 성공", page));

    assertThat(page.page()).isEqualTo(1);
    assertThat(page.content()).hasSize(2);
    assertThat(page.content().get(0).getEventId()).isEqualTo(ev2.getEventId());
  }
}