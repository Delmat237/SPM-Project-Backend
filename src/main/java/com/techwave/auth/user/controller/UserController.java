package com.techwave.auth.user.controller;

import com.techwave.auth.user.model.User;
import com.techwave.auth.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Utilisateurs (Profils)", description = "Endpoints de gestion des profils personnels des utilisateurs et actions d'administration des comptes")
public class UserController {

    @Autowired
    private UserService userService;

    // 🔹 Lister tous les utilisateurs (seuls les admins)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les utilisateurs (Admin)", description = "Retourne la liste complète de tous les comptes du système (rôle ADMIN requis)")
    public List<User> getAllUser() {
        return userService.getAllUser();
    }

    // 🔹 Mettre à jour son propre profil
    @PutMapping("/me")
    @Operation(summary = "Mettre à jour son propre profil", description = "Permet à l'utilisateur connecté de mettre à jour ses informations de profil (nom, pays, téléphone, etc.)")
    public ResponseEntity<User> updateMyProfile(@AuthenticationPrincipal User currentUser, @RequestBody User userDetails) {
        User updated = userService.updateUser(currentUser.getId(), userDetails);
        return ResponseEntity.ok(updated);
    }

    // 🔹 Désactiver son propre compte (traceability instead of hard delete)
    @DeleteMapping("/me")
    @Operation(summary = "Désactiver son propre compte", description = "Permet à l'utilisateur connecté de désactiver son propre compte (désactivation logique pour traçabilité)")
    public ResponseEntity<Void> deactivateMyAccount(@AuthenticationPrincipal User currentUser) {
        userService.deactivateUser(currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    // 🔹 Supprimer un utilisateur par ID (seuls les admins)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un utilisateur (Admin)", description = "Permet à un administrateur de supprimer un compte utilisateur par son identifiant unique")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // 🔹 Créer un utilisateur (seuls les admins)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un utilisateur (Admin)", description = "Permet à un administrateur de créer directement un nouveau compte utilisateur")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.status(201).body(created);
    }

    // 🔹 Mettre à jour un utilisateur par ID (seuls les admins)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un utilisateur (Admin)", description = "Permet à un administrateur de modifier les informations d'un utilisateur existant")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        User updated = userService.updateUser(id, userDetails);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }
}
