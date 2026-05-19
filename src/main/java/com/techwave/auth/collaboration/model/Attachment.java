package com.techwave.auth.collaboration.model;

import com.techwave.auth.project.model.Task;
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
@Table(name = "attachments", indexes = {
        @Index(name = "idx_attachment_task", columnList = "task_id")
})
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom affiché du fichier (nom original de l'upload).
     */
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    /**
     * Nom unique stocké sur le système de fichiers (UUID + extension).
     */
    @Column(name = "stored_file_name", nullable = false, unique = true)
    private String storedFileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    /**
     * Taille en octets.
     */
    @Column(name = "file_size", nullable = false)
    private long fileSize;

    /**
     * Chemin absolu vers le fichier stocké.
     */
    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
