package com.techwave.auth.collaboration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCommentRequest {

    @NotBlank(message = "Le contenu du commentaire est obligatoire")
    private String content;
}
