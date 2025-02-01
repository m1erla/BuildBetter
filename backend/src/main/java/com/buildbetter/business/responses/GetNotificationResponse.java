package com.renovatipoint.business.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.renovatipoint.entities.concretes.Notification;
import com.renovatipoint.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetNotificationResponse implements Serializable {
    private String id;
    private String title;
    private String message;
    private NotificationType type;
    private String relatedEntityId;
    private String timestamp;
    private boolean isRead;

    public static GetNotificationResponse fromEntity(Notification notification) {
        return GetNotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .relatedEntityId(notification.getRelatedEntityId())
                .timestamp(notification.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .isRead(notification.isRead())
                .build();
    }
}