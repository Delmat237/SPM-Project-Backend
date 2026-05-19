package com.techwave.auth.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * Vélocité de l'équipe par période (sprint / semaine).
 */
@Getter
@Builder
@AllArgsConstructor
public class VelocityResponse {

    private List<VelocityDataPoint> sprints;

    /** Moyenne de tâches complétées par sprint */
    private double averageVelocity;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class VelocityDataPoint {
        /** Libellé de la période (ex: "Semaine 20") */
        private String label;
        private LocalDate startDate;
        private LocalDate endDate;
        /** Nombre de tâches complétées dans cette période */
        private long completedTasks;
    }
}
