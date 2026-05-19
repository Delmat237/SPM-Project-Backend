package com.techwave.auth.common.exception;

/**
 * Exception métier (422 Unprocessable Entity).
 * Utilisée pour les erreurs de logique métier
 * (ex: transition d'état interdite, invitation déjà acceptée, etc.)
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
