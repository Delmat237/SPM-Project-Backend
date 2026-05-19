package com.techwave.auth.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * Données du burndown chart pour un sprint donné.
 */
@Getter
@Builder
@AllArgsConstructor
public class BurndownResponse {

    private LocalDate sprintStart;
    private LocalDate sprintEnd;
    private long totalTasks;
    private List<BurndownDataPoint> dataPoints;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class BurndownDataPoint {
        /** Date du point de données */
        private LocalDate date;
        /** Nombre de tâches restantes (non terminées) à cette date */
        private long remaining;
    }
}
