package com.techwave.auth.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRoleRequest {

    @NotBlank(message = "Le rôle est obligatoire")
    private String role; // "ADMIN", "MEMBER" ou "READER"
}
