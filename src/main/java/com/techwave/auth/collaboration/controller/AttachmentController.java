package com.techwave.auth.collaboration.controller;

import com.techwave.auth.collaboration.dto.AttachmentResponse;
import com.techwave.auth.collaboration.service.AttachmentService;
import com.techwave.auth.user.model.User;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Collaboration (Fichiers joints)", description = "Endpoints d'upload, de listing, de génération de liens temporaires signés et de téléchargement de pièces jointes")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    // =============================================
    // 🔹 POST /api/tasks/{id}/attachments — Upload d'un fichier
    // =============================================
    @PostMapping("/api/tasks/{taskId}/attachments")
    @Operation(summary = "Téléverser une pièce jointe", description = "Enregistre un fichier physique associé à la tâche spécifiée (limite par défaut de 100 Mo)")
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file) {

        AttachmentResponse attachment = attachmentService.uploadAttachment(taskId, currentUser, file);
        return ResponseEntity.status(201).body(attachment);
    }

    // =============================================
    // 🔹 GET /api/tasks/{id}/attachments — Liste des fichiers
    // =============================================
    @GetMapping("/api/tasks/{taskId}/attachments")
    @Operation(summary = "Lister les pièces jointes d'une tâche", description = "Retourne toutes les métadonnées des pièces jointes associées à la tâche")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long taskId) {

        List<AttachmentResponse> attachments = attachmentService.getAttachments(taskId, currentUser);
        return ResponseEntity.ok(attachments);
    }

    // =============================================
    // 🔹 GET /api/attachments/{id}/download — Obtenir l'URL signée
    // =============================================
    @GetMapping("/api/attachments/{attachmentId}/download")
    @Operation(summary = "Obtenir un lien de téléchargement signé", description = "Génère une URL publique temporaire et signée permettant de télécharger directement le fichier sans headers d'autorisation")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long attachmentId) {

        Map<String, String> result = attachmentService.getDownloadUrl(attachmentId, currentUser);
        return ResponseEntity.ok(result);
    }

    // =============================================
    // 🔹 GET /api/attachments/file/{token} — Télécharger via URL signée (public)
    // =============================================
    @GetMapping("/api/attachments/file/{token}")
    @Operation(summary = "Télécharger le fichier physique via URL signée (Public)", description = "Sert le fichier physique associé au jeton d'accès temporaire généré par l'endpoint d'URL signée")
    public ResponseEntity<Resource> downloadFile(@PathVariable String token) {

        AttachmentService.AttachmentDownload download = attachmentService.resolveDownloadToken(token);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.fileName() + "\"")
                .body(download.resource());
    }

    // =============================================
    // 🔹 DELETE /api/attachments/{id} — Supprimer un fichier
    // =============================================
    @DeleteMapping("/api/attachments/{attachmentId}")
    @Operation(summary = "Supprimer une pièce jointe", description = "Supprime le fichier physique du serveur et ses métadonnées en base de données (Auteur ou Admin du projet uniquement)")
    public ResponseEntity<Void> deleteAttachment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long attachmentId) {

        attachmentService.deleteAttachment(attachmentId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
