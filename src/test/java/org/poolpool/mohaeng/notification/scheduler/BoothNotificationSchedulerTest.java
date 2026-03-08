package org.poolpool.mohaeng.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothEntity;
import org.poolpool.mohaeng.notification.entity.NotificationEntity;
import org.poolpool.mohaeng.notification.entity.NotificationTypeEntity;
import org.poolpool.mohaeng.notification.repository.NotificationRepository;
import org.poolpool.mohaeng.notification.repository.NotificationTypeRepository;
import org.poolpool.mohaeng.notification.service.NotificationService;
import org.poolpool.mohaeng.notification.type.NotiTypeId;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.type.SignupType;
import org.poolpool.mohaeng.user.type.UserType;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;

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
@Import({BoothNotificationScheduler.class, BoothNotificationSchedulerTest.TestNotiConfig.class})
class BoothNotificationSchedulerTest {

    @Autowired BoothNotificationScheduler scheduler;

    @Autowired NotificationRepository notificationRepository;
    @Autowired NotificationTypeRepository notificationTypeRepository;

    @Autowired TestEntityManager em;

    @PersistenceContext EntityManager entityManager;

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
    void scanParticipationBoothAndNotify_creates_receiver_accept_reject_and_no_duplicates() {
        seedNotiType(NotiTypeId.BOOTH_RECEIVER, "BOOTH_RECEIVER", "[TITLE] 부스신청");
        seedNotiType(NotiTypeId.BOOTH_ACCEPT, "BOOTH_ACCEPT", "[TITLE] 승인");
        seedNotiType(NotiTypeId.BOOTH_REJECT, "BOOTH_REJECT", "[TITLE] 반려");

        UserEntity host = persistUser("host");
        EventEntity ev = persistEventWithHost("행사", host);
        Long hostBoothId = insertHostBooth(ev.getEventId());

        long applicantId = 200L;

        // 1) 신청 -> host에게 8
        ParticipationBoothEntity p1 = new ParticipationBoothEntity();
        p1.setHostBoothId(hostBoothId);
        p1.setUserId(applicantId);
        p1.setBoothTitle("부스");
        p1.setBoothTopic("토픽");
        p1.setMainItems("아이템");
        p1.setDescription("설명");
        em.persist(p1);
        em.flush();

        scheduler.scanParticipationBoothAndNotify();
        em.flush();

        long hostCnt1 = notificationRepository.countByUserId(host.getUserId());
        long appCnt1 = notificationRepository.countByUserId(applicantId);
        printApiJson("booth_apply_notify", true, "부스 신청 알림 생성(주최자)", new LinkedHashMap<String, Object>() {{
            put("hostUserId", host.getUserId());
            put("hostCount", hostCnt1);
            put("applicantId", applicantId);
            put("applicantCount", appCnt1);
        }});

        assertThat(hostCnt1).isEqualTo(1);
        assertThat(appCnt1).isEqualTo(0);

        // 2) 결제완료(=승인) -> 신청자에게 9
        p1.setStatus("결제완료");
        em.merge(p1);
        em.flush();

        scheduler.scanParticipationBoothAndNotify();
        em.flush();

        long appCnt2 = notificationRepository.countByUserId(applicantId);
        printApiJson("booth_accept_notify", true, "부스 승인 알림 생성(신청자)", new LinkedHashMap<String, Object>() {{
            put("applicantId", applicantId);
            put("applicantCount", appCnt2);
        }});
        assertThat(appCnt2).isEqualTo(1);

        // 3) 반려 -> 신청자에게 10
        ParticipationBoothEntity p2 = new ParticipationBoothEntity();
        p2.setHostBoothId(hostBoothId);
        p2.setUserId(applicantId);
        p2.setBoothTitle("부스2");
        p2.setBoothTopic("토픽2");
        p2.setMainItems("아이템2");
        p2.setDescription("설명2");
        p2.setStatus("반려");
        em.persist(p2);
        em.flush();

        scheduler.scanParticipationBoothAndNotify();
        em.flush();

        long appCnt3 = notificationRepository.countByUserId(applicantId);
        printApiJson("booth_reject_notify", true, "부스 반려 알림 생성(신청자)", new LinkedHashMap<String, Object>() {{
            put("applicantId", applicantId);
            put("applicantCount", appCnt3);
        }});
        assertThat(appCnt3).isEqualTo(2);

        // 4) 중복 방지
        scheduler.scanParticipationBoothAndNotify();
        em.flush();

        long hostCnt2 = notificationRepository.countByUserId(host.getUserId());
        long appCnt4 = notificationRepository.countByUserId(applicantId);
        printApiJson("booth_no_duplicates", true, "중복 생성 방지 확인", new LinkedHashMap<String, Object>() {{
            put("hostCount", hostCnt2);
            put("applicantCount", appCnt4);
        }});

        assertThat(hostCnt2).isEqualTo(1);
        assertThat(appCnt4).isEqualTo(2);
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

    private UserEntity persistUser(String name) {
        UserEntity u = UserEntity.builder()
            .email(name + "_" + System.nanoTime() + "@test.com")
            .name(name)
            .userType(UserType.PERSONAL)
            .signupType(SignupType.BASIC)
            .build();
        em.persist(u);
        em.flush();
        return u;
    }

    private EventEntity persistEventWithHost(String title, UserEntity host) {
        EventEntity ev = new EventEntity();
        ev.setHost(host);
        ev.setTitle(title);
        ev.setHasBooth(false);
        ev.setHasFacility(false);
        ev.setViews(0);
        em.persist(ev);
        em.flush();
        return ev;
    }

    private Long insertHostBooth(Long eventId) {
        entityManager.createNativeQuery("""
            insert into host_booth
                (booth_price, remain_count, total_count, created_at, event_id, booth_name, booth_size, booth_note)
            values
                (1000, 10, 10, now(6), :eventId, '기본부스', 'S', 'note')
        """)
        .setParameter("eventId", eventId)
        .executeUpdate();

        Object maxId = entityManager.createNativeQuery("select max(booth_id) from host_booth")
            .getSingleResult();
        return ((Number) maxId).longValue();
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