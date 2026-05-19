package com.techwave.auth.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeStatusRequest {

    /**
     * TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED
     */
    @NotBlank(message = "Le statut est obligatoire")
    private String status;
}
