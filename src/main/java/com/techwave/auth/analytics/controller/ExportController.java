package com.techwave.auth.analytics.controller;

import com.techwave.auth.analytics.dto.ExportJobResponse;
import com.techwave.auth.analytics.service.ExportJobService;
import com.techwave.auth.user.model.User;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.MalformedURLException;
import java.net.URI;

@RestController
@RequestMapping("/api/exports")
@Tag(name = "Analytics & Exports (Jobs)", description = "Endpoints pour suivre le statut et télécharger les exports de projets générés de manière asynchrone")
public class ExportController {

    private final ExportJobService exportJobService;

    public ExportController(ExportJobService exportJobService) {
        this.exportJobService = exportJobService;
    }

    // =============================================
    // 🔹 GET /api/exports/{jobId} — Statut du job
    // =============================================
    @GetMapping("/{jobId}")
    @Operation(summary = "Statut d'un job d'export", description = "Retourne le statut actuel (PENDING, DONE, FAILED) d'un job d'export de projet")
    public ResponseEntity<ExportJobResponse> getExportStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long jobId) {

        ExportJobResponse job = exportJobService.getExportJob(jobId, currentUser);
        return ResponseEntity.ok(job);
    }

    // =============================================
    // 🔹 GET /api/exports/{jobId}/download — Redirection vers le fichier
    // =============================================
    @GetMapping("/{jobId}/download")
    @Operation(summary = "Télécharger l'export de projet (Redirection)", description = "Redirige (status 302 + Location) vers l'URL signée publique de téléchargement disponible 24h")
    public ResponseEntity<Void> downloadExport(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long jobId) {

        String downloadUrl = exportJobService.getDownloadUrl(jobId, currentUser);

        return ResponseEntity
                .status(302)
                .location(URI.create(downloadUrl))
                .build();
    }

    // =============================================
    // 🔹 GET /api/exports/file/{token} — Télécharger via URL signée (public)
    // =============================================
    @GetMapping("/file/{token}")
    @Operation(summary = "Télécharger le fichier physique via URL signée (Public)", description = "Sert directement le fichier physique exporté à partir du token signé généré temporairement")
    public ResponseEntity<Resource> downloadExportFile(@PathVariable String token)
            throws MalformedURLException {

        ExportJobService.ExportDownload download = exportJobService.resolveDownloadToken(token);
        Resource resource = new UrlResource(download.filePath().toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.fileName() + "\"")
                .body(resource);
    }
}
