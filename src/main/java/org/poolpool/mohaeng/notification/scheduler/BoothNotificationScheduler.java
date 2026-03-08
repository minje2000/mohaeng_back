package org.poolpool.mohaeng.notification.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.poolpool.mohaeng.notification.service.NotificationService;
import org.poolpool.mohaeng.notification.type.NotiTypeId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BoothNotificationScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final Set<String> APPLY_SET  = Set.of("신청", "접수");
    private static final Set<String> REJECT_SET = Set.of("반려", "거절", "거부");
    private static final Set<String> ACCEPT_SET = Set.of("결제완료", "승인", "승인완료");

    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager em;

    // 5분마다 최근 변경분만 스캔
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    @Transactional
    public void scanParticipationBoothAndNotify() {
        LocalDateTime from = LocalDateTime.now(KST).minusMinutes(10);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
            select p.pct_booth_id, p.host_booth_id, p.user_id, p.status
              from participation_booth p
             where p.created_at >= :from
                or (p.updated_at is not null and p.updated_at >= :from)
        """)
        .setParameter("from", from)
        .getResultList();

        for (Object[] r : rows) {
            Long pctBoothId = ((Number) r[0]).longValue();
            Long hostBoothId = ((Number) r[1]).longValue();
            Long applicantId = ((Number) r[2]).longValue();
            String status = (String) r[3];

            Long eventId = findEventIdByHostBoothId(hostBoothId);
            if (eventId == null) continue;

            // 8) 신청 들어옴 -> 주최자에게
            if (status != null && APPLY_SET.contains(status)) {
                Long hostUserId = findHostUserIdByEventId(eventId);
                if (hostUserId != null && !existsNoti(hostUserId, NotiTypeId.BOOTH_RECEIVER, pctBoothId)) {
                    notificationService.createWithStatus(
                            hostUserId, NotiTypeId.BOOTH_RECEIVER, eventId, null,
                            String.valueOf(pctBoothId), status
                    );
                }
            }

            // 9) 승인/결제완료 -> 신청자에게
            if (status != null && ACCEPT_SET.contains(status)) {
                if (!existsNoti(applicantId, NotiTypeId.BOOTH_ACCEPT, pctBoothId)) {
                    notificationService.createWithStatus(
                            applicantId, NotiTypeId.BOOTH_ACCEPT, eventId, null,
                            String.valueOf(pctBoothId), status
                    );
                }
            }

            // 10) 반려 -> 신청자에게
            if (status != null && REJECT_SET.contains(status)) {
                if (!existsNoti(applicantId, NotiTypeId.BOOTH_REJECT, pctBoothId)) {
                    notificationService.createWithStatus(
                            applicantId, NotiTypeId.BOOTH_REJECT, eventId, null,
                            String.valueOf(pctBoothId), status
                    );
                }
            }
        }
    }

    private boolean existsNoti(Long userId, long notiTypeId, Long pctBoothId) {
        Long cnt = em.createQuery("""
            select count(n)
              from NotificationEntity n
             where n.userId = :userId
               and n.notiTypeId = :typeId
               and n.status1 = :key
        """, Long.class)
        .setParameter("userId", userId)
        .setParameter("typeId", notiTypeId)
        .setParameter("key", String.valueOf(pctBoothId))
        .getSingleResult();

        return cnt != null && cnt > 0;
    }

    private Long findEventIdByHostBoothId(Long hostBoothId) {
        List<?> res = em.createNativeQuery("""
            select hb.event_id
              from host_booth hb
             where hb.booth_id = :boothId
        """)
        .setParameter("boothId", hostBoothId)
        .getResultList();

        if (res.isEmpty()) return null;
        return ((Number) res.get(0)).longValue();
    }

    private Long findHostUserIdByEventId(Long eventId) {
        List<?> res = em.createNativeQuery("""
            select e.host_id
              from event e
             where e.event_id = :eventId
        """)
        .setParameter("eventId", eventId)
        .getResultList();

        if (res.isEmpty() || res.get(0) == null) return null;
        return ((Number) res.get(0)).longValue();
    }
}