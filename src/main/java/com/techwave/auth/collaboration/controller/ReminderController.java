package com.techwave.auth.collaboration.controller;

import com.techwave.auth.collaboration.dto.ReminderPreferenceRequest;
import com.techwave.auth.collaboration.dto.ReminderPreferenceResponse;
import com.techwave.auth.collaboration.service.ReminderService;
import com.techwave.auth.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/reminders")
@Tag(name = "Collaboration (Rappels)", description = "Préférences de rappels d'échéance (e-mail, in-app, push)")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping
    @Operation(summary = "Mes préférences de rappels", description = "Retourne les canaux de rappel activés et le délai avant échéance")
    public ResponseEntity<ReminderPreferenceResponse> getPreferences(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reminderService.getPreferences(currentUser));
    }

    @PutMapping
    @Operation(summary = "Mettre à jour mes préférences de rappels", description = "Active/désactive les canaux et ajuste le nombre de jours avant l'échéance")
    public ResponseEntity<ReminderPreferenceResponse> updatePreferences(
            @AuthenticationPrincipal User currentUser,
            @RequestBody ReminderPreferenceRequest request) {
        return ResponseEntity.ok(reminderService.updatePreferences(currentUser, request));
    }
}
