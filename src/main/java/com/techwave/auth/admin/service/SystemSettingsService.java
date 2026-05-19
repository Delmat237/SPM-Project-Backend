package com.techwave.auth.admin.service;

import com.techwave.auth.admin.dto.SystemSettingsResponse;
import com.techwave.auth.admin.model.SystemSetting;
import com.techwave.auth.admin.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SystemSettingsService {

    private final SystemSettingRepository settingRepository;

    public SystemSettingsService(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /**
     * Initialise les paramètres par défaut s'ils n'existent pas encore.
     */
    @PostConstruct
    @Transactional
    public void initDefaults() {
        createIfNotExists("max_projects_per_user", "50", "Nombre max de projets par utilisateur", "quotas");
        createIfNotExists("max_members_per_project", "100", "Nombre max de membres par projet", "quotas");
        createIfNotExists("max_file_size_mb", "100", "Taille max d'upload en Mo", "quotas");
        createIfNotExists("invitation_expiry_hours", "72", "Durée de validité des invitations en heures", "security");
        createIfNotExists("otp_expiry_minutes", "15", "Durée de validité des codes OTP en minutes", "security");
        createIfNotExists("jwt_expiration_ms", "1500000", "Durée du token JWT en ms", "security");
        createIfNotExists("app_name", "SPM", "Nom de l'application", "general");
        createIfNotExists("mail_from_name", "SPM – Gestion de Projets", "Nom d'expéditeur des emails", "mail");
        createIfNotExists("maintenance_mode", "false", "Mode maintenance activé", "general");
    }

    /**
     * Lire tous les paramètres, groupés par catégorie.
     */
    public SystemSettingsResponse getAllSettings() {
        List<SystemSetting> settings = settingRepository.findAllByOrderByCategoryAscSettingKeyAsc();
        return SystemSettingsResponse.from(settings);
    }

    /**
     * Modifier un ou plusieurs paramètres.
     */
    @Transactional
    public SystemSettingsResponse updateSettings(Map<String, String> updates) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            settingRepository.findBySettingKey(entry.getKey()).ifPresent(setting -> {
                setting.setSettingValue(entry.getValue());
                settingRepository.save(setting);
            });
        }
        return getAllSettings();
    }

    /**
     * Lire la valeur d'un paramètre (utilisable par d'autres services).
     */
    public String getValue(String key, String defaultValue) {
        return settingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    private void createIfNotExists(String key, String value, String description, String category) {
        if (settingRepository.findBySettingKey(key).isEmpty()) {
            settingRepository.save(new SystemSetting(key, value, description, category));
        }
    }
}
