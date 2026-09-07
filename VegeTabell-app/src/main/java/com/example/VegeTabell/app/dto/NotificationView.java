package com.example.VegeTabell.app.dto;

import com.example.VegeTabell.app.entity.Notification;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 通知一覧の1件分の表示用モデル。
 */
public record NotificationView(
        Long id,
        String title,
        String body,
        boolean read,
        String createdAtLabel
) {

    private static final DateTimeFormatter CREATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("M/d HH:mm").withZone(ZoneId.systemDefault());

    public static NotificationView from(Notification notification) {
        return new NotificationView(
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.isRead(),
                format(notification.getCreatedAt())
        );
    }

    private static String format(Instant createdAt) {
        return CREATED_AT_FORMAT.format(createdAt);
    }
}
