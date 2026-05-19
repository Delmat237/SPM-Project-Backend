package com.techwave.auth.collaboration.model;

/**
 * Types de notifications envoyées aux utilisateurs.
 */
public enum NotificationType {
    /** Un commentaire a été ajouté sur votre tâche */
    COMMENT_ADDED,
    /** Vous avez été mentionné dans un commentaire (@mention) */
    MENTION,
    /** Une tâche vous a été assignée */
    TASK_ASSIGNED,
    /** Vous avez reçu une invitation à un projet */
    INVITATION_RECEIVED,
    /** Le statut d'une tâche assignée a changé */
    TASK_STATUS_CHANGED
}
