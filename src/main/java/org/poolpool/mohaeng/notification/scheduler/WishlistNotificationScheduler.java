package org.poolpool.mohaeng.notification.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.poolpool.mohaeng.event.wishlist.entity.EventWishlistEntity;
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
public class WishlistNotificationScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager em;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void sendWishlistNotifications() {
        LocalDate today = LocalDate.now(KST);
        LocalDate tomorrow = today.plusDays(1);

        //  참여자 모집 시작 전날 / 당일
        sendForRecruitStartDate(tomorrow, NotiTypeId.EVENT_DAY_BEFORE); // 1
        sendForRecruitStartDate(today, NotiTypeId.EVENT_DAY_OF);       // 2
    }

    private void sendForRecruitStartDate(LocalDate recruitStartDate, long notiTypeId) {
        // 1) 해당 날짜 모집 시작 & DELETED 제외 이벤트
        List<Long> eventIds = em.createQuery("""
                select e.eventId
                  from EventEntity e
                 where e.startRecruit = :recruitStartDate
                   and (e.eventStatus is null or e.eventStatus <> 'DELETED')
                """, Long.class)
            .setParameter("recruitStartDate", recruitStartDate)
            .getResultList();

        if (eventIds.isEmpty()) return;

        // 2) 알림 ON 관심 유저
        List<EventWishlistEntity> wishList = em.createQuery("""
                select w
                  from EventWishlistEntity w
                 where w.notificationEnabled = true
                   and w.eventId in :eventIds
                """, EventWishlistEntity.class)
            .setParameter("eventIds", eventIds)
            .getResultList();

        if (wishList.isEmpty()) return;

        for (EventWishlistEntity w : wishList) {
            Long userId = w.getUserId();
            Long eventId = w.getEventId();

            boolean exists = em.createQuery("""
                    select count(n)
                      from NotificationEntity n
                     where n.userId = :userId
                       and n.notiTypeId = :notiTypeId
                       and n.eventId = :eventId
                    """, Long.class)
                .setParameter("userId", userId)
                .setParameter("notiTypeId", notiTypeId)
                .setParameter("eventId", eventId)
                .getSingleResult() > 0;

            if (exists) continue;

            notificationService.create(userId, notiTypeId, eventId, null);
        }
    }
}