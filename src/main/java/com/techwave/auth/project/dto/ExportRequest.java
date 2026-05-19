package com.techwave.auth.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExportRequest {

    @NotBlank(message = "Le format est obligatoire")
    private String format; // "CSV", "JSON" ou "PDF"

    private String scope; // "TASKS" ou "FULL", défaut FULL
}
