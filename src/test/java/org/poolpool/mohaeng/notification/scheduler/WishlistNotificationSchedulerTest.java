package org.poolpool.mohaeng.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.wishlist.entity.EventWishlistEntity;
import org.poolpool.mohaeng.notification.entity.NotificationEntity;
import org.poolpool.mohaeng.notification.entity.NotificationTypeEntity;
import org.poolpool.mohaeng.notification.repository.NotificationRepository;
import org.poolpool.mohaeng.notification.repository.NotificationTypeRepository;
import org.poolpool.mohaeng.notification.service.NotificationService;
import org.poolpool.mohaeng.notification.type.NotiTypeId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.persistence.EntityNotFoundException;

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/mohaeng_test?serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true",
    "spring.datasource.username=poolpool",
    "spring.datasource.password=...",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.jpa.show-sql=true"
})
@EntityScan(basePackages = {"org.poolpool.mohaeng"})
@EnableJpaRepositories(basePackages = {"org.poolpool.mohaeng"})
@Import({WishlistNotificationScheduler.class, WishlistNotificationSchedulerTest.TestNotiConfig.class})
class WishlistNotificationSchedulerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired WishlistNotificationScheduler scheduler;

    @Autowired NotificationRepository notificationRepository;
    @Autowired NotificationTypeRepository notificationTypeRepository;

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

    @Test
    void sendWishlistNotifications_creates_and_no_duplicates() {
        seedNotiType(NotiTypeId.EVENT_DAY_BEFORE, "EVENT_DAY_BEFORE", "관심 행사 [TITLE] 전날");
        seedNotiType(NotiTypeId.EVENT_DAY_OF, "EVENT_DAY_OF", "관심 행사 [TITLE] 당일");

        long userId = 1L;
        LocalDate today = LocalDate.now(KST);
        LocalDate tomorrow = today.plusDays(1);

        EventEntity evToday = persistEvent("오늘행사", today);
        persistWishlist(userId, evToday.getEventId(), true);

        EventEntity evTomorrow = persistEvent("내일행사", tomorrow);
        persistWishlist(userId, evTomorrow.getEventId(), true);

        em.flush();

        scheduler.sendWishlistNotifications();
        em.flush();

        long after1 = notificationRepository.countByUserId(userId);
        printApiJson("wishlist_scheduler_run_1", true, "위시 알림 생성(전날/당일) 완료", new LinkedHashMap<String, Object>() {{
            put("userId", userId);
            put("afterCount", after1);
        }});
        assertThat(after1).isEqualTo(2);

        scheduler.sendWishlistNotifications();
        em.flush();

        long after2 = notificationRepository.countByUserId(userId);
        printApiJson("wishlist_scheduler_run_2", true, "중복 생성 방지 확인", new LinkedHashMap<String, Object>() {{
            put("userId", userId);
            put("afterCount", after2);
        }});
        assertThat(after2).isEqualTo(2);
    }

    private void seedNotiType(long id, String name, String contents) {
        if (notificationTypeRepository.existsById(id)) return;
        notificationTypeRepository.save(
            NotificationTypeEntity.builder()
                .notiTypeId(id)
                .notiTypeName(name)
                .notiTypeContents(contents)
                .build()
        );
    }

    private EventEntity persistEvent(String title, LocalDate startDate) {
        EventEntity ev = new EventEntity();
        ev.setTitle(title);
        ev.setStartDate(startDate);
        ev.setHasBooth(false);
        ev.setHasFacility(false);
        ev.setViews(0);
        em.persist(ev);
        em.flush();
        return ev;
    }

    private EventWishlistEntity persistWishlist(long userId, long eventId, boolean enabled) {
        EventWishlistEntity w = new EventWishlistEntity();
        w.setUserId(userId);
        w.setEventId(eventId);
        w.setNotificationEnabled(enabled);
        em.persist(w);
        em.flush();
        return w;
    }

    @TestConfiguration
    static class TestNotiConfig {
        @Bean
        NotificationService notificationService(NotificationRepository notiRepo, NotificationTypeRepository typeRepo) {
            return new NotificationService() {
                @Override public long create(long userId, long notiTypeId, Long eventId, Long reportId) {
                    if (!typeRepo.existsById(notiTypeId)) throw new EntityNotFoundException("알림 타입 없음");
                    return notiRepo.save(NotificationEntity.builder()
                            .userId(userId).notiTypeId(notiTypeId).eventId(eventId).reportId(reportId).build()
                    ).getNotificationId();
                }
                @Override public long createWithStatus(long userId, long notiTypeId, Long eventId, Long reportId, String status1, String status2) {
                    if (!typeRepo.existsById(notiTypeId)) throw new EntityNotFoundException("알림 타입 없음");
                    return notiRepo.save(NotificationEntity.builder()
                            .userId(userId).notiTypeId(notiTypeId).eventId(eventId).reportId(reportId)
                            .status1(status1).status2(status2).build()
                    ).getNotificationId();
                }

                @Override public org.poolpool.mohaeng.common.api.PageResponse<org.poolpool.mohaeng.notification.dto.NotificationItemDto> getList(long userId, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
                @Override public long count(long userId) { return notiRepo.countByUserId(userId); }
                @Override public void read(long userId, long notificationId) { throw new UnsupportedOperationException(); }
                @Override public void readAll(long userId) { throw new UnsupportedOperationException(); }
            };
        }
    }
}