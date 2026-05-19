package com.techwave.auth.project.dto;

import com.techwave.auth.project.model.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Réponse pour la vue Gantt : tâches avec dates et dépendances.
 */
@Getter
@Builder
@AllArgsConstructor
public class GanttResponse {

    private List<GanttTask> tasks;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GanttTask {
        private Long id;
        private String taskKey;
        private String title;
        private String status;
        private String priority;
        private Long assigneeId;
        private String assigneeName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long parentId;
        private List<GanttDependency> dependencies;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GanttDependency {
        private Long taskId;
        private Long dependsOn;
    }

    public static GanttResponse from(List<Task> tasks) {
        List<GanttTask> ganttTasks = tasks.stream().map(task -> {
            List<GanttDependency> deps = task.getDependencies().stream()
                    .map(dep -> GanttDependency.builder()
                            .taskId(task.getId())
                            .dependsOn(dep.getId())
                            .build())
                    .collect(Collectors.toList());

            return GanttTask.builder()
                    .id(task.getId())
                    .taskKey(task.getTaskKey())
                    .title(task.getTitle())
                    .status(task.getStatus().name())
                    .priority(task.getPriority().name())
                    .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                    .assigneeName(task.getAssignee() != null ? task.getAssignee().getNom() : null)
                    .startDate(task.getStartDate())
                    .endDate(task.getDueDate())
                    .parentId(task.getParent() != null ? task.getParent().getId() : null)
                    .dependencies(deps)
                    .build();
        }).collect(Collectors.toList());

        return GanttResponse.builder().tasks(ganttTasks).build();
    }
}
