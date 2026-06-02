package com.techwave.auth.collaboration.dto;

import com.techwave.auth.user.model.User;

/**
 * Préférences de rappels d'échéance d'un utilisateur.
 */
public record ReminderPreferenceResponse(
        boolean emailEnabled,
        boolean inAppEnabled,
        boolean pushEnabled,
        int daysBefore
) {
    public static ReminderPreferenceResponse from(User user) {
        return new ReminderPreferenceResponse(
                user.isReminderEmailEnabled(),
                user.isReminderInAppEnabled(),
                user.isReminderPushEnabled(),
                user.getReminderDaysBefore()
        );
    }
}
