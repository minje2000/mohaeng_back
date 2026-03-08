package org.poolpool.mohaeng.event.wishlist.repository;

import org.poolpool.mohaeng.event.wishlist.entity.EventWishlistEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventWishlistRepository extends JpaRepository<EventWishlistEntity, Long> {

    // 마이페이지: 내가 등록한 관심행사 목록(최신순)
    Page<EventWishlistEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 관심 유/무 확인(중복 등록 방지)
    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    // 관심 해제(본인 것만)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByWishIdAndUserId(Long wishId, Long userId);

    // 알림 수신 유/무 토글(본인 것만)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update EventWishlistEntity w
           set w.notificationEnabled = :enabled
         where w.wishId = :wishId
           and w.userId = :userId
    """)
    int updateNotificationEnabledByWishIdAndUserId(
            @Param("wishId") Long wishId,
            @Param("userId") Long userId,
            @Param("enabled") boolean enabled
    );

    // 토글 후 최신 상태 조회
    Optional<EventWishlistEntity> findByWishIdAndUserId(Long wishId, Long userId);
}
