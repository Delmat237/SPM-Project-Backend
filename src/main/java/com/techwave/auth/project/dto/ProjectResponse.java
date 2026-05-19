package com.techwave.auth.project.dto;

import com.techwave.auth.project.model.Project;
import com.techwave.auth.project.model.ProjectMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private String projectKey;
    private String visibility;
    private boolean archived;
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private int memberCount;
    private String myRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project project, ProjectMember currentMember) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .projectKey(project.getProjectKey())
                .visibility(project.getVisibility().name())
                .archived(project.isArchived())
                .ownerId(project.getOwner().getId())
                .ownerName(project.getOwner().getNom())
                .ownerEmail(project.getOwner().getEmail())
                .memberCount(project.getMembers().size())
                .myRole(currentMember != null ? currentMember.getRole().name() : null)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
