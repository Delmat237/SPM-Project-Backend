package com.techwave.auth.collaboration.repository;

import com.techwave.auth.collaboration.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    /**
     * Fichiers joints à une tâche, triés par date de création décroissante.
     */
    @Query("SELECT a FROM Attachment a JOIN FETCH a.uploadedBy WHERE a.task.id = :taskId ORDER BY a.createdAt DESC")
    List<Attachment> findByTaskIdOrderByCreatedAtDesc(@Param("taskId") Long taskId);
}
