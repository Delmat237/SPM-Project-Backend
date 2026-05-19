package com.techwave.auth.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateUserAdminRequest {

    /**
     * Nouveau rôle global (ex: ["ADMIN", "USER"] ou ["USER"]).
     */
    private List<String> roles;

    /**
     * Activer/désactiver le compte.
     */
    private Boolean enabled;

    /**
     * Nom (modifiable par l'admin).
     */
    private String nom;
}
