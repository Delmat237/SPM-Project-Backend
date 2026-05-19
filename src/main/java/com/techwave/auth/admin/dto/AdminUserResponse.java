package com.techwave.auth.admin.dto;

import com.techwave.auth.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Vue détaillée d'un utilisateur pour les administrateurs.
 */
@Getter
@Builder
@AllArgsConstructor
public class AdminUserResponse {

    private Long id;
    private String email;
    private String nom;
    private String pays;
    private String telephone;
    private boolean enabled;
    private List<String> roles;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nom(user.getNom())
                .pays(user.getPays())
                .telephone(user.getTelephone())
                .enabled(user.isEnabled())
                .roles(user.getRoles() != null ? user.getRoles() : List.of())
                .build();
    }
}
