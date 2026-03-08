package org.poolpool.mohaeng.notification.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTIFICATION_ID")
    private Long notificationId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    //  DB 컬럼명: NOTITYPE_ID
    @Column(name = "NOTITYPE_ID", nullable = false)
    private Long notiTypeId;

    @Column(name = "EVENT_ID")
    private Long eventId;

    @Column(name = "REPORT_ID")
    private Long reportId;

    @Column(name = "STATUS1", length = 50)
    private String status1;

    @Column(name = "STATUS2", length = 50)
    private String status2;

    @Column(name = "EVENT_BEFORE_AT")
    private LocalDateTime eventBeforeAt;

    @Column(name = "EVENT_DAY_AT")
    private LocalDateTime eventDayAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}