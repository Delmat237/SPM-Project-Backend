package com.techwave.auth.collaboration.dto;

import com.techwave.auth.collaboration.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String message;
    private boolean read;

    // Références de navigation
    private Long relatedProjectId;
    private Long relatedTaskId;
    private Long relatedCommentId;

    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.isRead())
                .relatedProjectId(notification.getRelatedProjectId())
                .relatedTaskId(notification.getRelatedTaskId())
                .relatedCommentId(notification.getRelatedCommentId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
