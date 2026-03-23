package org.poolpool.mohaeng.notification.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationChangedEvent {
    private final long userId;
}