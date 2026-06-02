package com.techwave.auth.collaboration.dto;

/**
 * Mise à jour (partielle) des préférences de rappels.
 * Les champs nuls sont ignorés (la valeur existante est conservée).
 */
public record ReminderPreferenceRequest(
        Boolean emailEnabled,
        Boolean inAppEnabled,
        Boolean pushEnabled,
        Integer daysBefore
) {}
