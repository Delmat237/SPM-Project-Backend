package com.techwave.auth.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectRequest {
    private String name;
    private String description;
    private String visibility; // "PUBLIC" ou "PRIVATE"
}
