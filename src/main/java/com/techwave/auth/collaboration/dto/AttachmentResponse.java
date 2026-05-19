package com.techwave.auth.collaboration.dto;

import com.techwave.auth.collaboration.model.Attachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class AttachmentResponse {

    private Long id;
    private String fileName;
    private String contentType;
    private long fileSize;

    /**
     * Taille lisible (ex: "2.5 MB").
     */
    private String fileSizeFormatted;

    // Upload info
    private Long uploadedById;
    private String uploadedByName;
    private String uploadedByEmail;

    // Tâche
    private Long taskId;
    private String taskKey;

    private LocalDateTime createdAt;

    public static AttachmentResponse from(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .fileName(attachment.getOriginalFileName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .fileSizeFormatted(formatFileSize(attachment.getFileSize()))
                .uploadedById(attachment.getUploadedBy().getId())
                .uploadedByName(attachment.getUploadedBy().getNom())
                .uploadedByEmail(attachment.getUploadedBy().getEmail())
                .taskId(attachment.getTask().getId())
                .taskKey(attachment.getTask().getTaskKey())
                .createdAt(attachment.getCreatedAt())
                .build();
    }

    public static List<AttachmentResponse> fromList(List<Attachment> attachments) {
        return attachments.stream().map(AttachmentResponse::from).collect(Collectors.toList());
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
