package com.techwave.auth.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Format d'erreur standardisé conforme au spec :
 * { "status": 422, "message": "...", "errors": { "field": "message" } }
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private int status;
    private String message;
    private Map<String, String> errors;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static ApiErrorResponse of(int status, String message) {
        return ApiErrorResponse.builder()
                .status(status)
                .message(message)
                .build();
    }

    public static ApiErrorResponse of(int status, String message, Map<String, String> errors) {
        return ApiErrorResponse.builder()
                .status(status)
                .message(message)
                .errors(errors)
                .build();
    }
}
