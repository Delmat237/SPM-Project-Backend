package com.techwave.auth.project.controller;

import com.techwave.auth.project.dto.*;
import com.techwave.auth.project.service.ProjectService;
import com.techwave.auth.user.model.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projets", description = "Endpoints de gestion de cycle de vie des projets (création, modification, archivage, suppression) et gestion des membres")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // =============================================
    // 🔹 GET /api/projects — Liste de mes projets (paginée)
    // =============================================
    @GetMapping
    @Operation(summary = "Lister mes projets (Paginé)", description = "Retourne la liste paginée de tous les projets auxquels l'utilisateur connecté participe")
    public ResponseEntity<Page<ProjectResponse>> getMyProjects(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 100); // Taille max 100
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProjectResponse> projects = projectService.getMyProjects(currentUser, pageable);
        return ResponseEntity.ok(projects);
    }

    // =============================================
    // 🔹 POST /api/projects — Créer un nouveau projet
    // =============================================
    @PostMapping
    @Operation(summary = "Créer un nouveau projet", description = "Crée un projet vide avec l'utilisateur connecté comme chef de projet (rôle ADMIN)")
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateProjectRequest request) {

        ProjectResponse project = projectService.createProject(currentUser, request);
        return ResponseEntity.status(201).body(project);
    }

    // =============================================
    // 🔹 GET /api/projects/{id} — Détail d'un projet
    // =============================================
    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un projet", description = "Retourne les détails d'un projet spécifique par son identifiant unique")
    public ResponseEntity<ProjectResponse> getProject(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        ProjectResponse project = projectService.getProject(id, currentUser);
        return ResponseEntity.ok(project);
    }

    // =============================================
    // 🔹 PATCH /api/projects/{id} — Modifier un projet
    // =============================================
    @PatchMapping("/{id}")
    @Operation(summary = "Modifier un projet", description = "Permet de modifier le nom, la description ou la clé du projet (Chef de projet uniquement)")
    public ResponseEntity<ProjectResponse> updateProject(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestBody UpdateProjectRequest request) {

        ProjectResponse project = projectService.updateProject(id, currentUser, request);
        return ResponseEntity.ok(project);
    }

    // =============================================
    // 🔹 DELETE /api/projects/{id} — Supprimer (soft delete)
    // =============================================
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un projet (Soft Delete)", description = "Marque le projet comme supprimé sans effacer physiquement les données (Chef de projet uniquement)")
    public ResponseEntity<Void> deleteProject(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        projectService.deleteProject(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // =============================================
    // 🔹 PATCH /api/projects/{id}/archive — Archiver / désarchiver
    // =============================================
    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archiver ou désarchiver un projet", description = "Bascule l'état d'archivage du projet (les projets archivés sont en lecture seule)")
    public ResponseEntity<ProjectResponse> archiveProject(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestBody ArchiveRequest request) {

        ProjectResponse project = projectService.archiveProject(id, currentUser, request);
        return ResponseEntity.ok(project);
    }

    // =============================================
    // 🔹 GET /api/projects/{id}/members — Liste des membres
    // =============================================
    @GetMapping("/{id}/members")
    @Operation(summary = "Lister les membres du projet", description = "Retourne la liste complète des membres participants au projet avec leurs rôles respectifs")
    public ResponseEntity<List<MemberResponse>> getMembers(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        List<MemberResponse> members = projectService.getMembers(id, currentUser);
        return ResponseEntity.ok(members);
    }

    // =============================================
    // 🔹 POST /api/projects/{id}/members — Inviter un membre
    // =============================================
    @PostMapping("/{id}/members")
    @Operation(summary = "Inviter un nouveau membre", description = "Envoie une invitation par email avec un token unique et un rôle spécifique (Admin ou Membre du projet requis)")
    public ResponseEntity<InvitationResponse> addMember(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request) {

        InvitationResponse invitation = projectService.addMember(id, currentUser, request);
        return ResponseEntity.status(201).body(invitation);
    }

    // =============================================
    // 🔹 PATCH /api/projects/{id}/members/{userId} — Changer le rôle
    // =============================================
    @PatchMapping("/{id}/members/{userId}")
    @Operation(summary = "Modifier le rôle d'un membre", description = "Permet de modifier le rôle d'un membre du projet (Chef de projet uniquement)")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {

        MemberResponse member = projectService.updateMemberRole(id, userId, currentUser, request);
        return ResponseEntity.ok(member);
    }

    // =============================================
    // 🔹 DELETE /api/projects/{id}/members/{userId} — Retirer un membre
    // =============================================
    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Retirer un membre du projet", description = "Exclut un membre du projet (Chef de projet uniquement) et diffuse un événement temps réel")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @PathVariable Long userId) {

        projectService.removeMember(id, userId, currentUser);
        return ResponseEntity.noContent().build();
    }

    // Export : déplacé vers AnalyticsController (POST /api/projects/{id}/export) — version asynchrone avec jobId.
}
