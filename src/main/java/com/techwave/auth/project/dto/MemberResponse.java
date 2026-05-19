package com.techwave.auth.project.dto;

import com.techwave.auth.project.model.ProjectMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MemberResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private LocalDateTime joinedAt;

    public static MemberResponse from(ProjectMember member) {
        return MemberResponse.builder()
                .userId(member.getUser().getId())
                .email(member.getUser().getEmail())
                .fullName(member.getUser().getNom())
                .role(member.getRole().name())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
