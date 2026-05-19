package com.techwave.auth.analytics.model;

import com.techwave.auth.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "export_jobs", indexes = {
        @Index(name = "idx_export_user", columnList = "requested_by_id"),
        @Index(name = "idx_export_status", columnList = "status")
})
public class ExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID du projet à exporter.
     */
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * Format d'export : CSV ou JSON.
     */
    @Column(nullable = false, length = 10)
    private String format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExportStatus status = ExportStatus.PENDING;

    /**
     * Chemin du fichier généré (renseigné quand status = DONE).
     */
    @Column(name = "file_path")
    private String filePath;

    /**
     * Nom du fichier stocké (UUID + extension).
     */
    @Column(name = "stored_file_name")
    private String storedFileName;

    /**
     * Message d'erreur (renseigné quand status = FAILED).
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Expiration du fichier (24h après complétion).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
