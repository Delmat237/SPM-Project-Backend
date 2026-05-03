package com.techwave.auth.user.controller;

import com.techwave.auth.user.model.User;
import com.techwave.auth.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 🔹 Lister tous les utilisateurs (seuls les admins)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUser() {
        return userService.getAllUser();
    }

    // 🔹 Mettre à jour son propre profil
    @PutMapping("/me")
    public ResponseEntity<User> updateMyProfile(@AuthenticationPrincipal User currentUser, @RequestBody User userDetails) {
        User updated = userService.updateUser(currentUser.getId(), userDetails);
        return ResponseEntity.ok(updated);
    }

    // 🔹 Désactiver son propre compte (traceability instead of hard delete)
    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivateMyAccount(@AuthenticationPrincipal User currentUser) {
        userService.deactivateUser(currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    // 🔹 Supprimer un utilisateur par ID (seuls les admins)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // 🔹 Créer un utilisateur (seuls les admins)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.status(201).body(created);
    }

    // 🔹 Mettre à jour un utilisateur par ID (seuls les admins)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        User updated = userService.updateUser(id, userDetails);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }
}
