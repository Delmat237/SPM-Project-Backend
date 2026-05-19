package com.techwave.auth.admin.dto;

import com.techwave.auth.admin.model.SystemSetting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class SystemSettingsResponse {

    /**
     * Paramètres groupés par catégorie.
     */
    private Map<String, List<SettingEntry>> settings;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SettingEntry {
        private Long id;
        private String key;
        private String value;
        private String description;
        private String category;
        private LocalDateTime updatedAt;
    }

    public static SystemSettingsResponse from(List<SystemSetting> settings) {
        Map<String, List<SettingEntry>> grouped = settings.stream()
                .map(s -> SettingEntry.builder()
                        .id(s.getId())
                        .key(s.getSettingKey())
                        .value(s.getSettingValue())
                        .description(s.getDescription())
                        .category(s.getCategory())
                        .updatedAt(s.getUpdatedAt())
                        .build())
                .collect(Collectors.groupingBy(e -> e.getCategory() != null ? e.getCategory() : "general"));

        return SystemSettingsResponse.builder().settings(grouped).build();
    }
}
