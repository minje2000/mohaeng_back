package org.poolpool.mohaeng.notification.dto;

import java.time.LocalDateTime;

import org.poolpool.mohaeng.notification.entity.NotificationEntity;
import org.poolpool.mohaeng.notification.entity.NotificationTypeEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationItemDto {
    private Long notificationId;
    private Long notiTypeId;
    private String notiTypeName;
    private String contents;
    private LocalDateTime createdAt;

    public static NotificationItemDto fromEntity(NotificationEntity n, NotificationTypeEntity type, String contents) {
        return NotificationItemDto.builder()
                .notificationId(n.getNotificationId())
                .notiTypeId(n.getNotiTypeId())
                .notiTypeName(type != null ? type.getNotiTypeName() : "(알 수 없음)")
                .contents(contents)
                .createdAt(n.getCreatedAt())
                .build();
    }
}