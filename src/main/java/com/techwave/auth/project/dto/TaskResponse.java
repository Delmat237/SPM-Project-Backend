package com.techwave.auth.project.dto;

import com.techwave.auth.project.model.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private String taskKey;
    private String title;
    private String description;
    private String status;
    private String priority;
    private int orderIndex;

    // Assigné
    private Long assigneeId;
    private String assigneeName;
    private String assigneeEmail;

    // Projet
    private Long projectId;
    private String projectName;

    // Parent (sous-tâche)
    private Long parentId;
    private String parentTaskKey;
    private int subtaskCount;

    // Dates
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .taskKey(task.getTaskKey())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .orderIndex(task.getOrderIndex())
                // Assigné
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getNom() : null)
                .assigneeEmail(task.getAssignee() != null ? task.getAssignee().getEmail() : null)
                // Projet
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                // Parent
                .parentId(task.getParent() != null ? task.getParent().getId() : null)
                .parentTaskKey(task.getParent() != null ? task.getParent().getTaskKey() : null)
                .subtaskCount(task.getSubtasks() != null
                        ? (int) task.getSubtasks().stream().filter(s -> s.getDeletedAt() == null).count()
                        : 0)
                // Dates
                .startDate(task.getStartDate())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    public static List<TaskResponse> fromList(List<Task> tasks) {
        return tasks.stream().map(TaskResponse::from).collect(Collectors.toList());
    }
}
