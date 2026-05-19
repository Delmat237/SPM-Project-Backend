package com.techwave.auth.admin.model;

/**
 * Types d'événements tracés dans le journal d'audit.
 */
public enum AuditEventType {
    /** Connexion réussie */
    LOGIN,
    /** Déconnexion */
    LOGOUT,
    /** Création de ressource (projet, tâche, etc.) */
    CREATE,
    /** Modification de ressource */
    UPDATE,
    /** Suppression de ressource */
    DELETE,
    /** Export de données */
    EXPORT,
    /** Changement de rôle */
    ROLE_CHANGE,
    /** Activation / désactivation de compte */
    ACCOUNT_STATUS,
    /** Action RGPD (anonymisation, export de données) */
    GDPR,
    /** Modification des paramètres système */
    SETTINGS_CHANGE
}
