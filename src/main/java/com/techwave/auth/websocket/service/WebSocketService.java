package com.techwave.auth.websocket.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Envoie un événement temps réel de projet (tâches, membres) sur /topic/project/{projectId}
     */
    public void sendProjectEvent(Long projectId, String type, Object payload) {
        Map<String, Object> event = Map.of(
                "type", type,
                "payload", payload
        );
        messagingTemplate.convertAndSend("/topic/project/" + projectId, event);
    }

    /**
     * Envoie un événement de nouveau commentaire ou modification en temps réel sur /topic/task/{taskId}/comments
     */
    public void sendCommentEvent(Long taskId, String type, Object payload) {
        Map<String, Object> event = Map.of(
                "type", type,
                "payload", payload
        );
        messagingTemplate.convertAndSend("/topic/task/" + taskId + "/comments", event);
    }

    /**
     * Envoie une notification personnelle en temps réel sur /user/queue/notifications
     * (SockJS/STOMP résoudra l'utilisateur par son principal/username).
     */
    public void sendUserNotification(String userEmail, Object notification) {
        messagingTemplate.convertAndSendToUser(userEmail, "/queue/notifications", notification);
    }
}
