package com.techwave.auth.project.dto;

import com.techwave.auth.project.model.Invitation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class InvitationResponse {
    private Long id;
    private String email;
    private String role;
    private String status;
    private String projectName;
    private String invitedByName;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;

    public static InvitationResponse from(Invitation invitation) {
        return InvitationResponse.builder()
                .id(invitation.getId())
                .email(invitation.getEmail())
                .role(invitation.getRole().name())
                .status(invitation.getStatus().name())
                .projectName(invitation.getProject().getName())
                .invitedByName(invitation.getInvitedBy() != null ? invitation.getInvitedBy().getNom() : null)
                .expiryDate(invitation.getExpiryDate())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}
