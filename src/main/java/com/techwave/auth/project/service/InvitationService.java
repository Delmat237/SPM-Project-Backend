package com.techwave.auth.project.service;

import com.techwave.auth.common.exception.BusinessException;
import com.techwave.auth.common.exception.ResourceNotFoundException;
import com.techwave.auth.project.model.*;
import com.techwave.auth.project.repository.InvitationRepository;
import com.techwave.auth.project.repository.ProjectMemberRepository;
import com.techwave.auth.user.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final ProjectMemberRepository memberRepository;
    private final com.techwave.auth.websocket.service.WebSocketService webSocketService;

    public InvitationService(InvitationRepository invitationRepository,
                             ProjectMemberRepository memberRepository,
                             com.techwave.auth.websocket.service.WebSocketService webSocketService) {
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.webSocketService = webSocketService;
    }

    @Transactional
    public void acceptInvitation(String token, User user) {
        Invitation invitation = findPendingInvitation(token);

        // Vérifier que l'email correspond à l'utilisateur connecté
        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException("Cette invitation n'est pas destinée à votre compte");
        }

        // Vérifier si déjà membre
        if (memberRepository.existsByProjectAndUser(invitation.getProject(), user)) {
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitationRepository.save(invitation);
            throw new BusinessException("Vous êtes déjà membre de ce projet");
        }

        // Ajouter comme membre du projet
        ProjectMember member = new ProjectMember();
        member.setProject(invitation.getProject());
        member.setUser(user);
        member.setRole(invitation.getRole());
        ProjectMember savedMember = memberRepository.save(member);

        // Marquer l'invitation comme acceptée
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        try {
            webSocketService.sendProjectEvent(
                    invitation.getProject().getId(),
                    "member.joined",
                    com.techwave.auth.project.dto.MemberResponse.from(savedMember)
            );
        } catch (Exception e) {}
    }

    @Transactional
    public void declineInvitation(String token, User user) {
        Invitation invitation = findPendingInvitation(token);

        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException("Cette invitation n'est pas destinée à votre compte");
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        invitationRepository.save(invitation);
    }

    private Invitation findPendingInvitation(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation non trouvée"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException("Cette invitation a déjà été " +
                    (invitation.getStatus() == InvitationStatus.ACCEPTED ? "acceptée" : "refusée"));
        }

        if (invitation.getExpiryDate().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new BusinessException("Cette invitation a expiré");
        }

        return invitation;
    }
}
