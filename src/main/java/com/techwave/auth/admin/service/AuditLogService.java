package com.techwave.auth.admin.service;

import com.techwave.auth.admin.dto.AuditLogResponse;
import com.techwave.auth.admin.model.AuditEventType;
import com.techwave.auth.admin.model.AuditLog;
import com.techwave.auth.admin.repository.AuditLogRepository;
import com.techwave.auth.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Enregistrer un événement d'audit.
     */
    @Transactional
    public void log(User user, AuditEventType eventType, String description,
                    String resourceType, Long resourceId, String ipAddress) {
        AuditLog log = new AuditLog();
        if (user != null) {
            log.setUserId(user.getId());
            log.setUserEmail(user.getEmail());
        }
        log.setEventType(eventType);
        log.setDescription(description);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }

    /**
     * Enregistrer un événement d'audit avec métadonnées.
     */
    @Transactional
    public void log(User user, AuditEventType eventType, String description,
                    String resourceType, Long resourceId, String ipAddress, String metadata) {
        AuditLog auditLog = new AuditLog();
        if (user != null) {
            auditLog.setUserId(user.getId());
            auditLog.setUserEmail(user.getEmail());
        }
        auditLog.setEventType(eventType);
        auditLog.setDescription(description);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setIpAddress(ipAddress);
        auditLog.setMetadata(metadata);
        auditLogRepository.save(auditLog);
    }

    /**
     * Rechercher les logs avec filtres optionnels.
     */
    public Page<AuditLogResponse> getLogs(Long userId, String eventType,
                                           LocalDateTime from, LocalDateTime to,
                                           int page, int size) {
        AuditEventType parsedType = null;
        if (eventType != null && !eventType.isBlank()) {
            try {
                parsedType = AuditEventType.valueOf(eventType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignorer le filtre invalide
            }
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return auditLogRepository.findLogs(userId, parsedType, from, to, pageable)
                .map(AuditLogResponse::from);
    }

    /**
     * Exporter les logs en CSV (pour le bouton d'export admin).
     */
    public String exportLogsCsv(Long userId, String eventType,
                                 LocalDateTime from, LocalDateTime to) {
        AuditEventType parsedType = null;
        if (eventType != null && !eventType.isBlank()) {
            try {
                parsedType = AuditEventType.valueOf(eventType.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Pageable pageable = PageRequest.of(0, 10000); // Max 10k pour CSV
        Page<AuditLog> logs = auditLogRepository.findLogs(userId, parsedType, from, to, pageable);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Date,UserId,UserEmail,EventType,Description,ResourceType,ResourceId,IP\n");
        for (AuditLog log : logs.getContent()) {
            csv.append(log.getId()).append(",");
            csv.append(log.getCreatedAt()).append(",");
            csv.append(log.getUserId() != null ? log.getUserId() : "").append(",");
            csv.append(escapeCsv(log.getUserEmail())).append(",");
            csv.append(log.getEventType().name()).append(",");
            csv.append(escapeCsv(log.getDescription())).append(",");
            csv.append(log.getResourceType() != null ? log.getResourceType() : "").append(",");
            csv.append(log.getResourceId() != null ? log.getResourceId() : "").append(",");
            csv.append(log.getIpAddress() != null ? log.getIpAddress() : "").append("\n");
        }
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
