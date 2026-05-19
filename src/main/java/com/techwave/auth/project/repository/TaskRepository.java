package com.techwave.auth.project.repository;

import com.techwave.auth.project.model.Project;
import com.techwave.auth.project.model.Task;
import com.techwave.auth.project.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Trouve une tâche active par ID et projet.
     */
    @Query("SELECT t FROM Task t WHERE t.id = :id AND t.project.id = :projectId AND t.deletedAt IS NULL")
    Optional<Task> findActiveByIdAndProject(@Param("id") Long id, @Param("projectId") Long projectId);

    /**
     * Trouve une tâche (y compris supprimée) par ID et projet — pour la restauration.
     */
    @Query("SELECT t FROM Task t WHERE t.id = :id AND t.project.id = :projectId")
    Optional<Task> findByIdAndProject(@Param("id") Long id, @Param("projectId") Long projectId);

    /**
     * Liste paginée des tâches actives d'un projet (filtres optionnels).
     */
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.deletedAt IS NULL" +
            " AND (:status IS NULL OR t.status = :status)" +
            " AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)" +
            " AND (:priority IS NULL OR t.priority = :priority)" +
            " AND (:parentId IS NULL OR t.parent.id = :parentId)" +
            " ORDER BY t.orderIndex ASC, t.createdAt DESC")
    Page<Task> findTasks(@Param("projectId") Long projectId,
                         @Param("status") TaskStatus status,
                         @Param("assigneeId") Long assigneeId,
                         @Param("priority") com.techwave.auth.project.model.TaskPriority priority,
                         @Param("parentId") Long parentId,
                         Pageable pageable);

    /**
     * Toutes les tâches actives d'un projet (non paginé, pour Kanban/Gantt).
     * N'inclut que les tâches racines (sans parent) par défaut.
     */
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.deletedAt IS NULL" +
            " ORDER BY t.orderIndex ASC, t.createdAt DESC")
    List<Task> findAllActiveTasks(@Param("projectId") Long projectId);

    /**
     * Sous-tâches actives d'une tâche parent.
     */
    @Query("SELECT t FROM Task t WHERE t.parent.id = :parentId AND t.deletedAt IS NULL" +
            " ORDER BY t.orderIndex ASC, t.createdAt DESC")
    List<Task> findActiveSubtasks(@Param("parentId") Long parentId);

    /**
     * Toutes les sous-tâches (y compris supprimées) pour cascade de suppression.
     */
    @Query("SELECT t FROM Task t WHERE t.parent.id = :parentId")
    List<Task> findAllSubtasks(@Param("parentId") Long parentId);

    /**
     * Compteur de tâches existantes pour un projet (pour générer la clé).
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId")
    long countByProject(@Param("projectId") Long projectId);

    /**
     * Ordre max dans une colonne (statut) pour un projet.
     */
    @Query("SELECT COALESCE(MAX(t.orderIndex), 0) FROM Task t " +
            "WHERE t.project.id = :projectId AND t.status = :status AND t.deletedAt IS NULL")
    int findMaxOrderIndex(@Param("projectId") Long projectId, @Param("status") TaskStatus status);

    // =============================================
    // Requêtes Analytics
    // =============================================

    /**
     * Nombre de tâches actives par statut dans un projet.
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.deletedAt IS NULL AND t.status = :status")
    long countByProjectAndStatus(@Param("projectId") Long projectId, @Param("status") TaskStatus status);

    /**
     * Nombre de tâches en retard (dueDate passée et non terminées).
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.deletedAt IS NULL " +
            "AND t.dueDate < :today AND t.status <> 'DONE'")
    long countOverdueTasks(@Param("projectId") Long projectId, @Param("today") LocalDate today);

    /**
     * Tâches complétées (DONE) dans un intervalle de dates.
     * Utilise updatedAt comme proxy de la date de complétion.
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.deletedAt IS NULL " +
            "AND t.status = 'DONE' AND t.updatedAt >= :start AND t.updatedAt < :end")
    long countCompletedTasksBetween(@Param("projectId") Long projectId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    /**
     * Tâches créées avant une date donnée dans un projet (pour le burndown).
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.deletedAt IS NULL " +
            "AND t.createdAt <= :before")
    long countTasksCreatedBefore(@Param("projectId") Long projectId, @Param("before") LocalDateTime before);

    /**
     * Tâches complétées avant une date donnée dans un projet (pour le burndown).
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.deletedAt IS NULL " +
            "AND t.status = 'DONE' AND t.updatedAt <= :before")
    long countCompletedTasksBefore(@Param("projectId") Long projectId, @Param("before") LocalDateTime before);
}

