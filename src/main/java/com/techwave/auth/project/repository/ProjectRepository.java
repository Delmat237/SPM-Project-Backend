package com.techwave.auth.project.repository;

import com.techwave.auth.project.model.Project;
import com.techwave.auth.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Trouve les projets dont l'utilisateur est membre (non supprimés).
     */
    @Query("SELECT p FROM Project p JOIN p.members m WHERE m.user = :user AND p.deletedAt IS NULL")
    Page<Project> findMyProjects(@Param("user") User user, Pageable pageable);

    /**
     * Trouve un projet par ID (non supprimé).
     */
    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Project> findActiveById(@Param("id") Long id);

    /**
     * Vérifie si une clé de projet existe déjà.
     */
    boolean existsByProjectKey(String projectKey);

    /**
     * Trouve les projets supprimés depuis plus de 30 jours (pour purge).
     */
    @Query("SELECT p FROM Project p WHERE p.deletedAt IS NOT NULL AND p.deletedAt < :cutoff")
    List<Project> findProjectsToHardDelete(@Param("cutoff") LocalDateTime cutoff);
}
