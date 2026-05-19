package com.techwave.auth.analytics.dto;

import com.techwave.auth.analytics.model.ExportJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ExportJobResponse {

    private Long jobId;
    private String status;
    private String format;
    private Long projectId;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;

    public static ExportJobResponse from(ExportJob job) {
        return ExportJobResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus().name())
                .format(job.getFormat())
                .projectId(job.getProjectId())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .expiresAt(job.getExpiresAt())
                .build();
    }
}
