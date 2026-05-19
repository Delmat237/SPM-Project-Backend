package com.techwave.auth.admin.service;

import com.techwave.auth.admin.dto.AdminUserResponse;
import com.techwave.auth.admin.dto.UpdateUserAdminRequest;
import com.techwave.auth.admin.model.AuditEventType;
import com.techwave.auth.common.exception.BusinessException;
import com.techwave.auth.common.exception.ResourceNotFoundException;
import com.techwave.auth.user.model.User;
import com.techwave.auth.user.model.UserRole;
import com.techwave.auth.user.repository.UserRepository;
import com.techwave.auth.user.service.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    public AdminService(UserRepository userRepository,
                        AuditLogService auditLogService,
                        EmailService emailService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
    }

    /**
     * Liste paginée de tous les utilisateurs.
     */
    public Page<AdminUserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("id").ascending());
        return userRepository.findAll(pageable).map(AdminUserResponse::from);
    }

    /**
     * Détail d'un utilisateur.
     */
    public AdminUserResponse getUser(Long userId) {
        User user = findUser(userId);
        return AdminUserResponse.from(user);
    }

    /**
     * Modifier un utilisateur (rôle, activation, nom).
     */
    @Transactional
    public AdminUserResponse updateUser(Long userId, UpdateUserAdminRequest request, User admin, String ipAddress) {
        User user = findUser(userId);

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            List<String> newRoles = request.getRoles().stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
            user.setRoles(newRoles);

            auditLogService.log(admin, AuditEventType.ROLE_CHANGE,
                    "Rôles de " + user.getEmail() + " modifiés en " + newRoles,
                    "User", userId, ipAddress);
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());

            auditLogService.log(admin, AuditEventType.ACCOUNT_STATUS,
                    "Compte " + user.getEmail() + (request.getEnabled() ? " activé" : " désactivé"),
                    "User", userId, ipAddress);
        }

        if (request.getNom() != null) {
            user.setNom(request.getNom());
        }

        user = userRepository.save(user);
        return AdminUserResponse.from(user);
    }

    /**
     * Supprimer un utilisateur (RGPD — anonymisation).
     * Anonymise les données personnelles au lieu de supprimer physiquement
     * pour préserver l'intégrité des projets/tâches existants.
     */
    @Transactional
    public void deleteUser(Long userId, User admin, String ipAddress) {
        User user = findUser(userId);

        if (user.getId().equals(admin.getId())) {
            throw new BusinessException("Vous ne pouvez pas supprimer votre propre compte admin");
        }

        String originalEmail = user.getEmail();

        // Anonymiser les données personnelles
        String anonymizedId = UUID.randomUUID().toString().substring(0, 8);
        user.setEmail("deleted_" + anonymizedId + "@anonymized.spm");
        user.setNom("Utilisateur supprimé");
        user.setPays(null);
        user.setTelephone(null);
        user.setPassword("DELETED_ACCOUNT");
        user.setEnabled(false);
        user.setRoles(List.of());
        userRepository.save(user);

        // Log d'audit RGPD
        auditLogService.log(admin, AuditEventType.GDPR,
                "Compte utilisateur anonymisé (RGPD) — email original : " + originalEmail,
                "User", userId, ipAddress);

        // Email de confirmation (à l'adresse originale)
        try {
            emailService.sendGdprDeletionConfirmation(originalEmail);
        } catch (Exception e) {
            // Ne pas bloquer la suppression si l'email échoue
        }
    }

    /**
     * Export des données personnelles d'un utilisateur (RGPD — portabilité).
     * Retourne les données au format JSON.
     */
    public String exportUserData(Long userId, User admin, String ipAddress) {
        User user = findUser(userId);

        auditLogService.log(admin, AuditEventType.GDPR,
                "Export des données personnelles de " + user.getEmail() + " (RGPD)",
                "User", userId, ipAddress);

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"exportDate\": \"").append(LocalDateTime.now()).append("\",\n");
        json.append("  \"user\": {\n");
        json.append("    \"id\": ").append(user.getId()).append(",\n");
        json.append("    \"email\": \"").append(escapeJson(user.getEmail())).append("\",\n");
        json.append("    \"nom\": \"").append(escapeJson(user.getNom())).append("\",\n");
        json.append("    \"pays\": ").append(user.getPays() != null ? "\"" + escapeJson(user.getPays()) + "\"" : "null").append(",\n");
        json.append("    \"telephone\": ").append(user.getTelephone() != null ? "\"" + escapeJson(user.getTelephone()) + "\"" : "null").append(",\n");
        json.append("    \"enabled\": ").append(user.isEnabled()).append(",\n");
        json.append("    \"roles\": [");
        if (user.getRoles() != null) {
            json.append(user.getRoles().stream()
                    .map(r -> "\"" + escapeJson(r) + "\"")
                    .collect(Collectors.joining(", ")));
        }
        json.append("]\n");
        json.append("  }\n");
        json.append("}\n");

        return json.toString();
    }

    // =============================================
    // Utilitaires
    // =============================================

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
