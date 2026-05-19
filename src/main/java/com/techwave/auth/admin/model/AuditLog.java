package com.techwave.auth.admin.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_event", columnList = "event_type"),
        @Index(name = "idx_audit_created", columnList = "created_at")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID de l'utilisateur ayant déclenché l'événement.
     * Stocké comme Long (pas de FK) pour survivre à la suppression du compte.
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Email au moment de l'événement (snapshot pour audit post-suppression).
     */
    @Column(name = "user_email")
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuditEventType eventType;

    /**
     * Description lisible de l'événement.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Ressource concernée (ex: "Project", "Task", "User").
     */
    @Column(name = "resource_type", length = 50)
    private String resourceType;

    /**
     * ID de la ressource concernée.
     */
    @Column(name = "resource_id")
    private Long resourceId;

    /**
     * Adresse IP du client.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Métadonnées supplémentaires au format JSON.
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
