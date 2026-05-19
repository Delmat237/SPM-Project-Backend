package com.techwave.auth.collaboration.repository;

import com.techwave.auth.collaboration.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Commentaires d'une tâche, triés par date de création.
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.task.id = :taskId ORDER BY c.createdAt ASC")
    List<Comment> findByTaskIdOrderByCreatedAtAsc(@Param("taskId") Long taskId);
}
