package com.techwave.auth.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectRequest {

    @NotBlank(message = "Le nom du projet est obligatoire")
    private String name;

    private String description;

    private String visibility; // "PUBLIC" ou "PRIVATE", défaut PRIVATE
}
