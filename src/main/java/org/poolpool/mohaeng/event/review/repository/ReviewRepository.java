package org.poolpool.mohaeng.event.review.repository;

import java.util.Optional;

import org.poolpool.mohaeng.event.review.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    Page<ReviewEntity> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ReviewEntity> findByEvent_EventIdAndUser_UserIdNotOrderByCreatedAtDesc(Long eventId, Long userId, Pageable pageable);

    Optional<ReviewEntity> findByUser_UserIdAndEvent_EventId(Long userId, Long eventId);

    Optional<ReviewEntity> findByReviewIdAndUser_UserId(Long reviewId, Long userId);

    int deleteByReviewIdAndUser_UserId(Long reviewId, Long userId);

    boolean existsByUser_UserIdAndEvent_EventId(Long userId, Long eventId);
}