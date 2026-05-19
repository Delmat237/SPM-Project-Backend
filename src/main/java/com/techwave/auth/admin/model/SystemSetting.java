package com.techwave.auth.admin.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Paramètre système global (clé-valeur persisté en BDD).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", unique = true, nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    /**
     * Description de ce paramètre.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Catégorie pour le regroupement (ex: "quotas", "mail", "security").
     */
    @Column(length = 50)
    private String category;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public SystemSetting(String key, String value, String description, String category) {
        this.settingKey = key;
        this.settingValue = value;
        this.description = description;
        this.category = category;
    }
}
