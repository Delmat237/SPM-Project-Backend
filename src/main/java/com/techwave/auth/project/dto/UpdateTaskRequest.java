package com.techwave.auth.project.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateTaskRequest {

    private String title;
    private String description;

    /**
     * LOW, MEDIUM, HIGH, CRITICAL
     */
    private String priority;

    private Long assigneeId;

    private LocalDate startDate;
    private LocalDate dueDate;

    /**
     * Ordre d'affichage dans la colonne Kanban.
     */
    private Integer orderIndex;
}
