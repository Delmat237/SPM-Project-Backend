package com.techwave.auth.collaboration.repository;

import com.techwave.auth.collaboration.model.Notification;
import com.techwave.auth.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Notifications d'un utilisateur (toutes), paginées et triées par date décroissante.
     */
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Notifications d'un utilisateur filtrées par statut lu/non-lu.
     */
    Page<Notification> findByUserAndReadOrderByCreatedAtDesc(User user, boolean read, Pageable pageable);

    /**
     * Marquer toutes les notifications d'un utilisateur comme lues.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user AND n.read = false")
    int markAllAsRead(@Param("user") User user);

    /**
     * Compter les notifications non-lues d'un utilisateur.
     */
    long countByUserAndRead(User user, boolean read);
}
