package com.techwave.auth.project.controller;

import com.techwave.auth.project.dto.*;
import com.techwave.auth.project.service.TaskService;
import com.techwave.auth.user.model.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@Tag(name = "Tâches", description = "Endpoints de gestion des tâches, sous-tâches, vues Kanban et Gantt, et transition FSM de statut")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // =============================================
    // 🔹 GET /api/projects/{pId}/tasks — Liste des tâches
    //    Filtres : ?status=TODO&assignee=1&priority=HIGH&parentId=5
    //    Vues :   ?view=kanban | ?view=gantt
    // =============================================
    @GetMapping
    @Operation(summary = "Lister les tâches (Standard / Kanban / Gantt)", description = "Retourne soit une liste filtrée et paginée des tâches, soit la structure Kanban (groupée par statut) ou Gantt (tâches ordonnées avec dépendances)")
    public ResponseEntity<?> getTasks(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId,
            @RequestParam(required = false) String view,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assignee,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Vue Kanban
        if ("kanban".equalsIgnoreCase(view)) {
            KanbanResponse kanban = taskService.getKanbanView(projectId, currentUser);
            return ResponseEntity.ok(kanban);
        }

        // Vue Gantt
        if ("gantt".equalsIgnoreCase(view)) {
            GanttResponse gantt = taskService.getGanttView(projectId, currentUser);
            return ResponseEntity.ok(gantt);
        }

        // Vue liste standard (paginée avec filtres)
        Page<TaskResponse> tasks = taskService.getTasks(
                projectId, currentUser, status, assignee, priority, parentId, page, size);
        return ResponseEntity.ok(tasks);
    }

    // =============================================
    // 🔹 POST /api/projects/{pId}/tasks — Créer une tâche
    // =============================================
    @PostMapping
    @Operation(summary = "Créer une nouvelle tâche", description = "Crée une tâche principale rattachée au projet")
    public ResponseEntity<TaskResponse> createTask(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request) {

        TaskResponse task = taskService.createTask(projectId, currentUser, request);
        return ResponseEntity.status(201).body(task);
    }

    // =============================================
    // 🔹 GET /api/projects/{pId}/tasks/{id} — Détail d'une tâche
    // =============================================
    @GetMapping("/{taskId}")
    @Operation(summary = "Détail d'une tâche", description = "Retourne les informations détaillées d'une tâche spécifique")
    public ResponseEntity<TaskResponse> getTask(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId,
            @PathVariable Long taskId) {

        TaskResponse task = taskService.getTask(projectId, taskId, currentUser);
        return ResponseEntity.ok(task);
    }

    // =============================================
    // 🔹 PATCH /api/projects/{pId}/tasks/{id} — Modifier une tâche
    // =============================================
    @PatchMapping("/{taskId}")
    @Operation(summary = "Modifier une tâche", description = "Modifie partiellement les champs d'une tâche (titre, description, priorité, assigné, dates, etc.)")
    public ResponseEntity<TaskResponse> updateTask(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody UpdateTaskRequest request) {

        TaskResponse task = taskService.updateTask(projectId, taskId, currentUser, request);
        return ResponseEntity.ok(task);
    }

    // =============================================
    // 🔹 DELETE /api/projects/{pId}/tasks/{id} — Supprimer (soft delete)
    // =============================================
    @DeleteMapping("/{taskId}")
    @Operation(summary = "Supprimer une tâche (Soft Delete)", description = "Marque la tâche comme supprimée et la déplace dans la corbeille du projet")
    public ResponseEntity<Void> deleteTask(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId,
            @PathVariable Long taskId) {

        taskService.deleteTask(projectId, taskId, currentUser);
        return ResponseEntity.noContent().build();
    }

    // =============================================
    // 🔹 PATCH /api/projects/{pId}/tasks/{id}/status — Changer le statut (FSM)
    // =============================================
    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Changer le statut de la tâche (FSM)", description = "Met à jour le statut de la tâche en validant la transition via la machine à états (FSM)")
    public ResponseEntity<TaskResponse> changeStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody ChangeStatusRequest request) {

        TaskResponse task = taskService.changeStatus(projectId, taskId, currentUser, request);
        return ResponseEntity.ok(task);
    }

    // =============================================
    // 🔹 PATCH /api/projects/{pId}/tasks/{id}/restore — Restaurer
    // =============================================
    @PatchMapping("/{taskId}/restore")
    @Operation(summary = "Restaurer une tâche depuis la corbeille", description = "Restaure une tâche précédemment supprimée (soft delete)")
    public ResponseEntity<TaskResponse> restoreTask(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId,
            @PathVariable Long taskId) {

        TaskResponse task = taskService.restoreTask(projectId, taskId, currentUser);
        return ResponseEntity.ok(task);
    }

    // =============================================
    // 🔹 GET /api/projects/{pId}/tasks/{id}/subtasks — Liste des sous-tâches
    // =============================================
    @GetMapping("/{taskId}/subtasks")
    @Operation(summary = "Lister les sous-tâches", description = "Retourne la liste des sous-tâches dépendant de cette tâche")
    public ResponseEntity<List<TaskResponse>> getSubtasks(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId,
            @PathVariable Long taskId) {

        List<TaskResponse> subtasks = taskService.getSubtasks(projectId, taskId, currentUser);
        return ResponseEntity.ok(subtasks);
    }

    // =============================================
    // 🔹 POST /api/projects/{pId}/tasks/{id}/subtasks — Créer une sous-tâche
    // =============================================
    @PostMapping("/{taskId}/subtasks")
    @Operation(summary = "Créer une sous-tâche", description = "Crée une nouvelle sous-tâche rattachée à cette tâche parent")
    public ResponseEntity<TaskResponse> createSubtask(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody CreateTaskRequest request) {

        TaskResponse subtask = taskService.createSubtask(projectId, taskId, currentUser, request);
        return ResponseEntity.status(201).body(subtask);
    }
}
