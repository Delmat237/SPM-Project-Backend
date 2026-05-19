package com.techwave.auth.admin.repository;

import com.techwave.auth.admin.model.AuditEventType;
import com.techwave.auth.admin.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Recherche paginée de logs avec filtres optionnels.
     */
    @Query("SELECT l FROM AuditLog l WHERE " +
            "(:userId IS NULL OR l.userId = :userId) AND " +
            "(:eventType IS NULL OR l.eventType = :eventType) AND " +
            "(:from IS NULL OR l.createdAt >= :from) AND " +
            "(:to IS NULL OR l.createdAt <= :to) " +
            "ORDER BY l.createdAt DESC")
    Page<AuditLog> findLogs(@Param("userId") Long userId,
                            @Param("eventType") AuditEventType eventType,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            Pageable pageable);
}
