package com.techwave.auth.project.repository;

import com.techwave.auth.project.model.Invitation;
import com.techwave.auth.project.model.InvitationStatus;
import com.techwave.auth.project.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByToken(String token);

    Optional<Invitation> findByProjectAndEmailAndStatus(Project project, String email, InvitationStatus status);

    List<Invitation> findByEmailAndStatus(String email, InvitationStatus status);

    boolean existsByProjectAndEmailAndStatus(Project project, String email, InvitationStatus status);
}
