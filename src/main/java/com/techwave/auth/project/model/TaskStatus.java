package com.techwave.auth.project.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Statuts possibles d'une tâche avec machine à états (FSM).
 * Transitions autorisées :
 *   TODO        → IN_PROGRESS, BLOCKED
 *   IN_PROGRESS → IN_REVIEW, TODO, BLOCKED
 *   IN_REVIEW   → DONE, IN_PROGRESS, BLOCKED
 *   DONE        → TODO (réouverture)
 *   BLOCKED     → TODO, IN_PROGRESS
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE,
    BLOCKED;

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = Map.of(
            TODO,        EnumSet.of(IN_PROGRESS, BLOCKED),
            IN_PROGRESS, EnumSet.of(IN_REVIEW, TODO, BLOCKED),
            IN_REVIEW,   EnumSet.of(DONE, IN_PROGRESS, BLOCKED),
            DONE,        EnumSet.of(TODO),
            BLOCKED,     EnumSet.of(TODO, IN_PROGRESS)
    );

    /**
     * Vérifie si la transition vers le statut cible est autorisée.
     */
    public boolean canTransitionTo(TaskStatus target) {
        if (this == target) return false; // Pas de transition vers soi-même
        Set<TaskStatus> allowed = TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }
}
