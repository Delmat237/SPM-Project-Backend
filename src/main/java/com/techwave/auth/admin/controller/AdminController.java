package com.techwave.auth.admin.controller;

import com.techwave.auth.admin.dto.*;
import com.techwave.auth.admin.service.AdminService;
import com.techwave.auth.admin.service.AuditLogService;
import com.techwave.auth.admin.service.SystemSettingsService;
import com.techwave.auth.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administration", description = "Endpoints réservés aux administrateurs pour la gestion des utilisateurs, conformité RGPD, logs d'audit et configuration système")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;
    private final SystemSettingsService settingsService;

    public AdminController(AdminService adminService,
                            AuditLogService auditLogService,
                            SystemSettingsService settingsService) {
        this.adminService = adminService;
        this.auditLogService = auditLogService;
        this.settingsService = settingsService;
    }

    // =============================================
    // 👥 Gestion des utilisateurs
    // =============================================

    /**
     * GET /api/admin/users — Liste paginée de tous les utilisateurs.
     */
    @GetMapping("/users")
    @Operation(summary = "Lister tous les utilisateurs (Paginé)", description = "Retourne la liste paginée de tous les comptes utilisateurs enregistrés dans le système")
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }

    /**
     * GET /api/admin/users/{id} — Détail d'un utilisateur.
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Détail d'un utilisateur", description = "Retourne les informations détaillées d'un compte utilisateur (champs admin)")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUser(userId));
    }

    /**
     * PATCH /api/admin/users/{id} — Modifier rôle / activer / désactiver.
     */
    @PatchMapping("/users/{userId}")
    @Operation(summary = "Modifier un compte utilisateur", description = "Permet de modifier le rôle global de l'utilisateur ou d'activer/désactiver le compte")
    public ResponseEntity<AdminUserResponse> updateUser(
            @AuthenticationPrincipal User admin,
            @PathVariable Long userId,
            @RequestBody UpdateUserAdminRequest request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        return ResponseEntity.ok(adminService.updateUser(userId, request, admin, ip));
    }

    /**
     * DELETE /api/admin/users/{id} — Suppression RGPD (anonymisation).
     */
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Supprimer un utilisateur (Anonymisation RGPD)", description = "Déclenche la procédure RGPD d'effacement : anonymisation de toutes les données personnelles et envoi d'un mail de confirmation")
    public ResponseEntity<Map<String, String>> deleteUser(
            @AuthenticationPrincipal User admin,
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        adminService.deleteUser(userId, admin, ip);
        return ResponseEntity.ok(Map.of(
                "message", "Compte utilisateur anonymisé conformément au RGPD",
                "userId", userId.toString()
        ));
    }

    /**
     * GET /api/admin/users/{id}/export-data — Export des données personnelles (RGPD).
     */
    @GetMapping("/users/{userId}/export-data")
    @Operation(summary = "Exporter les données personnelles (Portabilité RGPD)", description = "Génère un fichier JSON contenant l'intégralité des données personnelles stockées pour l'utilisateur")
    public ResponseEntity<byte[]> exportUserData(
            @AuthenticationPrincipal User admin,
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        String jsonData = adminService.exportUserData(userId, admin, ip);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"user_" + userId + "_data_export.json\"");

        return ResponseEntity.ok().headers(headers).body(jsonData.getBytes());
    }

    // =============================================
    // 📋 Journal d'activité (Audit Logs)
    // =============================================

    /**
     * GET /api/admin/logs — Journal d'activité filtrable et paginé.
     * Filtres : ?userId=...&eventType=LOGIN|CREATE|DELETE&from=...&to=...
     */
    @GetMapping("/logs")
    @Operation(summary = "Consulter le journal d'activité (Audit Logs)", description = "Retourne la liste paginée et filtrée de tous les événements de sécurité et actions administratives critiques")
    public ResponseEntity<Page<AuditLogResponse>> getLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        return ResponseEntity.ok(auditLogService.getLogs(userId, eventType, from, to, page, size));
    }

    /**
     * GET /api/admin/logs/export — Exporter les logs en CSV.
     */
    @GetMapping("/logs/export")
    @Operation(summary = "Exporter les logs d'audit en CSV", description = "Télécharge un fichier CSV contenant les logs filtrés selon les mêmes critères que l'endpoint d'affichage")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        String csv = auditLogService.exportLogsCsv(userId, eventType, from, to);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_logs.csv\"");

        return ResponseEntity.ok().headers(headers).body(csv.getBytes());
    }

    // =============================================
    // ⚙️ Paramètres système
    // =============================================

    /**
     * GET /api/admin/settings — Lire la configuration globale.
     */
    @GetMapping("/settings")
    @Operation(summary = "Lire la configuration globale du système", description = "Retourne tous les paramètres clés-valeurs d'administration (quotas de stockage, paramètres d'email, etc.)")
    public ResponseEntity<SystemSettingsResponse> getSettings() {
        return ResponseEntity.ok(settingsService.getAllSettings());
    }

    /**
     * PATCH /api/admin/settings — Modifier les paramètres.
     * Body : { "key1": "value1", "key2": "value2" }
     */
    @PatchMapping("/settings")
    @Operation(summary = "Modifier les paramètres globaux", description = "Permet de modifier plusieurs clés de configuration système en une seule requête")
    public ResponseEntity<SystemSettingsResponse> updateSettings(
            @AuthenticationPrincipal User admin,
            @RequestBody Map<String, String> updates,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        auditLogService.log(admin, com.techwave.auth.admin.model.AuditEventType.SETTINGS_CHANGE,
                "Paramètres système modifiés : " + updates.keySet(),
                "SystemSettings", null, ip);

        return ResponseEntity.ok(settingsService.updateSettings(updates));
    }

    // =============================================
    // Utilitaire
    // =============================================

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Si X-Forwarded-For contient plusieurs IPs, prendre la première
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
