package com.techwave.auth.collaboration.controller;

import com.techwave.auth.collaboration.dto.NotificationResponse;
import com.techwave.auth.collaboration.service.NotificationService;
import com.techwave.auth.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Collaboration (Notifications)", description = "Endpoints de gestion et marquage des notifications personnelles des utilisateurs")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // =============================================
    // 🔹 GET /api/notifications — Mes notifications (paginées)
    //    Query: ?read=false pour non-lues uniquement
    // =============================================
    @GetMapping
    @Operation(summary = "Lister mes notifications (Paginé)", description = "Retourne la liste paginée de toutes les notifications de l'utilisateur connecté, filtrables par statut de lecture")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<NotificationResponse> notifications =
                notificationService.getMyNotifications(currentUser, read, page, size);
        return ResponseEntity.ok(notifications);
    }

    // =============================================
    // 🔹 PATCH /api/notifications/{id}/read — Marquer comme lue
    // =============================================
    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Marquer une notification comme lue", description = "Définit le statut de lecture d'une notification spécifique à 'lue'")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long notificationId) {

        NotificationResponse notification = notificationService.markAsRead(notificationId, currentUser);
        return ResponseEntity.ok(notification);
    }

    // =============================================
    // 🔹 PATCH /api/notifications/read-all — Marquer toutes comme lues
    // =============================================
    @PatchMapping("/read-all")
    @Operation(summary = "Marquer toutes les notifications comme lues", description = "Marque d'un coup toutes les notifications non-lues de l'utilisateur connecté comme lues")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal User currentUser) {

        int count = notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok(Map.of(
                "message", "Toutes les notifications ont été marquées comme lues",
                "count", count
        ));
    }
}
