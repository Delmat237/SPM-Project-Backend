package com.techwave.auth.analytics.controller;

import com.techwave.auth.analytics.dto.BurndownResponse;
import com.techwave.auth.analytics.dto.ExportJobResponse;
import com.techwave.auth.analytics.dto.ProjectSummaryResponse;
import com.techwave.auth.analytics.dto.VelocityResponse;
import com.techwave.auth.analytics.service.AnalyticsService;
import com.techwave.auth.analytics.service.ExportJobService;
import com.techwave.auth.user.model.User;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}")
@Tag(name = "Analytics & Rapports", description = "Endpoints d'analyse du projet, burndown, vélocité et génération d'exports asynchrones")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ExportJobService exportJobService;

    public AnalyticsController(AnalyticsService analyticsService,
                                ExportJobService exportJobService) {
        this.analyticsService = analyticsService;
        this.exportJobService = exportJobService;
    }

    // =============================================
    // 🔹 GET /api/projects/{id}/analytics/summary
    // =============================================
    @GetMapping("/analytics/summary")
    @Operation(summary = "Résumé analytique du projet", description = "Retourne le total des tâches, tâches complétées, en cours, en retard, et le taux d'avancement")
    public ResponseEntity<ProjectSummaryResponse> getSummary(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId) {

        ProjectSummaryResponse summary = analyticsService.getSummary(projectId, currentUser);
        return ResponseEntity.ok(summary);
    }

    // =============================================
    // 🔹 GET /api/projects/{id}/analytics/burndown
    //    Query: ?sprintStart=YYYY-MM-DD&sprintEnd=YYYY-MM-DD
    // =============================================
    @GetMapping("/analytics/burndown")
    @Operation(summary = "Données du burndown chart par sprint", description = "Retourne la liste quotidienne des tâches restantes sur un intervalle de dates donné")
    public ResponseEntity<BurndownResponse> getBurndown(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sprintStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sprintEnd) {

        BurndownResponse burndown = analyticsService.getBurndown(projectId, currentUser, sprintStart, sprintEnd);
        return ResponseEntity.ok(burndown);
    }

    // =============================================
    // 🔹 GET /api/projects/{id}/analytics/velocity
    // =============================================
    @GetMapping("/analytics/velocity")
    @Operation(summary = "Vélocité de l'équipe", description = "Retourne la vélocité moyenne de l'équipe (tâches complétées par sprint ou par semaine sur les 12 dernières semaines)")
    public ResponseEntity<VelocityResponse> getVelocity(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId) {

        VelocityResponse velocity = analyticsService.getVelocity(projectId, currentUser);
        return ResponseEntity.ok(velocity);
    }

    // =============================================
    // 🔹 POST /api/projects/{id}/export — Lancer un export async
    // =============================================
    @PostMapping("/export")
    @Operation(summary = "Lancer un export asynchrone du projet", description = "Démarre la génération en arrière-plan d'un export au format CSV ou JSON et retourne un jobId pour le polling")
    public ResponseEntity<ExportJobResponse> createExport(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long projectId,
            @RequestBody(required = false) Map<String, String> body) {

        String format = body != null ? body.getOrDefault("format", "CSV") : "CSV";
        ExportJobResponse job = exportJobService.createExportJob(projectId, format, currentUser);
        return ResponseEntity.status(202).body(job);  // 202 Accepted
    }
}
