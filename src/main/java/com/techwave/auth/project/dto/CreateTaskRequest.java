package com.techwave.auth.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateTaskRequest {

    @NotBlank(message = "Le titre de la tâche est obligatoire")
    private String title;

    private String description;

    /**
     * LOW, MEDIUM, HIGH, CRITICAL — défaut MEDIUM
     */
    private String priority;

    private Long assigneeId;

    private LocalDate startDate;

    private LocalDate dueDate;

    /**
     * ID de la tâche parent (pour créer une sous-tâche).
     */
    private Long parentId;
}
