package com.techwave.auth.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Réponse pour la vue Kanban : tâches groupées par colonne (statut).
 */
@Getter
@Builder
@AllArgsConstructor
public class KanbanResponse {

    private List<KanbanColumn> columns;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class KanbanColumn {
        private String id;       // ex: "TODO"
        private String name;     // ex: "À faire"
        private List<TaskResponse> tasks;
    }
}
