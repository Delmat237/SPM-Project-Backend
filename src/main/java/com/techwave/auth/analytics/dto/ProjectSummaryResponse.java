package com.techwave.auth.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Résumé analytique d'un projet.
 */
@Getter
@Builder
@AllArgsConstructor
public class ProjectSummaryResponse {

    private long totalTasks;
    private long completedTasks;
    private long inProgressTasks;
    private long todoTasks;
    private long blockedTasks;
    private long inReviewTasks;
    private long overdueCount;

    /**
     * Taux de complétion en pourcentage (0.0 — 100.0).
     */
    private double completionRate;

    /**
     * Nombre total de membres du projet.
     */
    private int memberCount;
}
