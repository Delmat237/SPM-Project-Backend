package com.techwave.auth.analytics.repository;

import com.techwave.auth.analytics.model.ExportJob;
import com.techwave.auth.analytics.model.ExportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {

    /**
     * Exports expirés à nettoyer.
     */
    @Query("SELECT e FROM ExportJob e WHERE e.status = 'DONE' AND e.expiresAt < :now")
    List<ExportJob> findExpiredExports(@Param("now") LocalDateTime now);
}
