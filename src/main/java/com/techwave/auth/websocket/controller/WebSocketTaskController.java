package com.techwave.auth.websocket.controller;

import com.techwave.auth.project.dto.ChangeStatusRequest;
import com.techwave.auth.project.dto.TaskResponse;
import com.techwave.auth.project.dto.UpdateTaskRequest;
import com.techwave.auth.project.service.TaskService;
import com.techwave.auth.user.model.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class WebSocketTaskController {

    private final TaskService taskService;

    public WebSocketTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Payload pour le drag & drop Kanban.
     */
    @Getter
    @Setter
    public static class MoveTaskPayload {
        private Long projectId;
        private String toStatus;
        private Integer orderIndex;
    }

    /**
     * Payload pour le changement de statut simple.
     */
    @Getter
    @Setter
    public static class ChangeStatusPayload {
        private Long projectId;
        private String status;
    }

    /**
     * SEND /app/task/{id}/move — Déplacer une carte Kanban (drag & drop)
     */
    @MessageMapping("/task/{taskId}/move")
    public void moveTask(@DestinationVariable Long taskId, MoveTaskPayload payload, Principal principal) {
        User currentUser = getUserFromPrincipal(principal);
        if (currentUser == null || payload.getProjectId() == null) return;

        // 1. Changer le statut via la FSM si spécifié
        if (payload.getToStatus() != null) {
            ChangeStatusRequest statusRequest = new ChangeStatusRequest();
            statusRequest.setStatus(payload.getToStatus());
            taskService.changeStatus(payload.getProjectId(), taskId, currentUser, statusRequest);
        }

        // 2. Changer l'index d'ordre si spécifié
        if (payload.getOrderIndex() != null) {
            UpdateTaskRequest updateRequest = new UpdateTaskRequest();
            updateRequest.setOrderIndex(payload.getOrderIndex());
            taskService.updateTask(payload.getProjectId(), taskId, currentUser, updateRequest);
        }
    }

    /**
     * SEND /app/task/{id}/status — Changer le statut d'une tâche via WS
     */
    @MessageMapping("/task/{taskId}/status")
    public void changeStatus(@DestinationVariable Long taskId, ChangeStatusPayload payload, Principal principal) {
        User currentUser = getUserFromPrincipal(principal);
        if (currentUser == null || payload.getProjectId() == null || payload.getStatus() == null) return;

        ChangeStatusRequest request = new ChangeStatusRequest();
        request.setStatus(payload.getStatus());
        taskService.changeStatus(payload.getProjectId(), taskId, currentUser, request);
    }

    private User getUserFromPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            Object rawUser = ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
            if (rawUser instanceof User) {
                return (User) rawUser;
            }
        }
        return null;
    }
}
