package com.techwave.auth.collaboration.service;

import com.techwave.auth.collaboration.model.Notification;
import com.techwave.auth.collaboration.model.NotificationType;
import com.techwave.auth.collaboration.repository.NotificationRepository;
import com.techwave.auth.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.techwave.auth.collaboration.dto.NotificationResponse;
import com.techwave.auth.common.exception.ResourceNotFoundException;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final com.techwave.auth.websocket.service.WebSocketService webSocketService;

    public NotificationService(NotificationRepository notificationRepository,
                               com.techwave.auth.websocket.service.WebSocketService webSocketService) {
        this.notificationRepository = notificationRepository;
        this.webSocketService = webSocketService;
    }

    // =============================================
    // Lecture des notifications
    // =============================================

    /**
     * Mes notifications paginées, optionnellement filtrées par statut lu/non-lu.
     */
    public Page<NotificationResponse> getMyNotifications(User user, Boolean readFilter, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        Page<Notification> notifications;
        if (readFilter != null) {
            notifications = notificationRepository.findByUserAndReadOrderByCreatedAtDesc(user, readFilter, pageable);
        } else {
            notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        }

        return notifications.map(NotificationResponse::from);
    }

    /**
     * Marquer une notification comme lue.
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification non trouvée");
        }

        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return NotificationResponse.from(notification);
    }

    /**
     * Marquer toutes mes notifications comme lues.
     */
    @Transactional
    public int markAllAsRead(User user) {
        return notificationRepository.markAllAsRead(user);
    }

    // =============================================
    // Création de notifications (appelé par d'autres services)
    // =============================================

    /**
     * Créer et enregistrer une notification.
     */
    @Transactional
    public Notification createNotification(User recipient, NotificationType type,
                                           String title, String message,
                                           Long projectId, Long taskId, Long commentId) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedProjectId(projectId);
        notification.setRelatedTaskId(taskId);
        notification.setRelatedCommentId(commentId);
        
        Notification saved = notificationRepository.save(notification);
        try {
            webSocketService.sendUserNotification(recipient.getEmail(), NotificationResponse.from(saved));
        } catch (Exception e) {
            // Ne pas bloquer en cas d'erreur de diffusion
        }
        return saved;
    }

    /**
     * Nombre de notifications non-lues.
     */
    public long countUnread(User user) {
        return notificationRepository.countByUserAndRead(user, false);
    }
}
