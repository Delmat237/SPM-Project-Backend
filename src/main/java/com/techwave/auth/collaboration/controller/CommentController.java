package com.techwave.auth.collaboration.controller;

import com.techwave.auth.collaboration.dto.CommentResponse;
import com.techwave.auth.collaboration.dto.CreateCommentRequest;
import com.techwave.auth.collaboration.dto.UpdateCommentRequest;
import com.techwave.auth.collaboration.service.CommentService;
import com.techwave.auth.user.model.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@Tag(name = "Collaboration (Commentaires)", description = "Endpoints de gestion des commentaires sur les tâches avec support des @mentions et push temps réel")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // =============================================
    // 🔹 GET /api/tasks/{id}/comments — Liste des commentaires
    // =============================================
    @GetMapping("/api/tasks/{taskId}/comments")
    @Operation(summary = "Lister les commentaires d'une tâche", description = "Retourne la liste complète de tous les commentaires postés pour la tâche spécifiée")
    public ResponseEntity<List<CommentResponse>> getComments(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long taskId) {

        List<CommentResponse> comments = commentService.getComments(taskId, currentUser);
        return ResponseEntity.ok(comments);
    }

    // =============================================
    // 🔹 POST /api/tasks/{id}/comments — Ajouter un commentaire
    // =============================================
    @PostMapping("/api/tasks/{taskId}/comments")
    @Operation(summary = "Ajouter un commentaire à une tâche", description = "Crée un nouveau commentaire. Détecte les @mentions dans le contenu pour notifier automatiquement les utilisateurs mentionnés")
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request) {

        CommentResponse comment = commentService.createComment(taskId, currentUser, request);
        return ResponseEntity.status(201).body(comment);
    }

    // =============================================
    // 🔹 PATCH /api/comments/{id} — Modifier un commentaire
    // =============================================
    @PatchMapping("/api/comments/{commentId}")
    @Operation(summary = "Modifier un commentaire", description = "Permet à l'auteur original de modifier le contenu textuel de son commentaire")
    public ResponseEntity<CommentResponse> updateComment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {

        CommentResponse comment = commentService.updateComment(commentId, currentUser, request);
        return ResponseEntity.ok(comment);
    }

    // =============================================
    // 🔹 DELETE /api/comments/{id} — Supprimer un commentaire
    // =============================================
    @DeleteMapping("/api/comments/{commentId}")
    @Operation(summary = "Supprimer un commentaire", description = "Permet de supprimer définitivement un commentaire (réservé à son auteur)")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long commentId) {

        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
