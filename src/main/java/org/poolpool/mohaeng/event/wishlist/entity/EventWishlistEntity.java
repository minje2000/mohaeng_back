package org.poolpool.mohaeng.event.wishlist.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "event_wishlist",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_wishlist_user_event", columnNames = {"user_id", "event_id"})
    }
)
public class EventWishlistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wishId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 등록일
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        // 기본값(원하면 true로 고정)
        // this.notificationEnabled = true;
    }

    // 기본 생성자
    public EventWishlistEntity() {}


    public EventWishlistEntity(Long wishId, Long userId, Long eventId, boolean notificationEnabled, LocalDateTime createdAt) {
        this.wishId = wishId;
        this.userId = userId;
        this.eventId = eventId;
        this.notificationEnabled = notificationEnabled;
        this.createdAt = createdAt;
    }

    // getters
    public long getWishId() { return wishId; }
    public long getUserId() { return userId; }
    public long getEventId() { return eventId; }
    public boolean isNotificationEnabled() { return notificationEnabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // setters
    public void setWishId(long wishId) { this.wishId = wishId; }
    public void setUserId(long userId) { this.userId = userId; }
    public void setEventId(long eventId) { this.eventId = eventId; }
    public void setNotificationEnabled(boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
