package com.techwave.auth.collaboration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequest {

    /**
     * Contenu du commentaire. Supporte les @mentions (ex: @user@email.com).
     */
    @NotBlank(message = "Le contenu du commentaire est obligatoire")
    private String content;
}
