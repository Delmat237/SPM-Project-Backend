package com.techwave.auth.project.controller;

import com.techwave.auth.project.service.InvitationService;
import com.techwave.auth.user.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@RestController
@RequestMapping("/api/invitations")
@Tag(name = "Invitations", description = "Endpoints permettant d'accepter ou de refuser les invitations de projet envoyées par email")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    // =============================================
    // 🔹 POST /api/invitations/{token}/accept — Accepter une invitation
    // =============================================
    @PostMapping("/{token}/accept")
    @Operation(summary = "Accepter une invitation de projet", description = "Valide le token d'invitation et ajoute l'utilisateur connecté comme membre du projet concerné")
    public ResponseEntity<Map<String, String>> acceptInvitation(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String token) {

        invitationService.acceptInvitation(token, currentUser);
        return ResponseEntity.ok(Map.of("message", "Invitation acceptée avec succès"));
    }

    // =============================================
    // 🔹 POST /api/invitations/{token}/decline — Refuser une invitation
    // =============================================
    @PostMapping("/{token}/decline")
    @Operation(summary = "Refuser une invitation de projet", description = "Valide le token et marque l'invitation comme refusée par l'utilisateur connecté")
    public ResponseEntity<Map<String, String>> declineInvitation(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String token) {

        invitationService.declineInvitation(token, currentUser);
        return ResponseEntity.ok(Map.of("message", "Invitation refusée"));
    }
}
