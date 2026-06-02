package com.techwave.auth.collaboration.service;

import com.techwave.auth.collaboration.dto.ReminderPreferenceRequest;
import com.techwave.auth.collaboration.dto.ReminderPreferenceResponse;
import com.techwave.auth.collaboration.model.NotificationType;
import com.techwave.auth.common.exception.ResourceNotFoundException;
import com.techwave.auth.project.model.Task;
import com.techwave.auth.project.repository.TaskRepository;
import com.techwave.auth.user.model.User;
import com.techwave.auth.user.repository.UserRepository;
import com.techwave.auth.user.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Gère les préférences de rappels et le planificateur qui envoie les rappels
 * d'échéance par e-mail et notification in-app.
 */
@Service
public class ReminderService {

    /** Fenêtre maximale (en jours) sur laquelle le planificateur regarde en avant. */
    private static final int MAX_HORIZON_DAYS = 30;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final String frontendUrl;

    public ReminderService(TaskRepository taskRepository,
                           UserRepository userRepository,
                           NotificationService notificationService,
                           EmailService emailService,
                           @Value("${app.frontend.url}") String frontendUrl) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    // =============================================
    // Préférences
    // =============================================

    public ReminderPreferenceResponse getPreferences(User user) {
        return ReminderPreferenceResponse.from(user);
    }

    @Transactional
    public ReminderPreferenceResponse updatePreferences(User user, ReminderPreferenceRequest req) {
        User managed = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (req.emailEnabled() != null) managed.setReminderEmailEnabled(req.emailEnabled());
        if (req.inAppEnabled() != null) managed.setReminderInAppEnabled(req.inAppEnabled());
        if (req.pushEnabled() != null)  managed.setReminderPushEnabled(req.pushEnabled());
        if (req.daysBefore() != null) {
            int d = Math.max(0, Math.min(MAX_HORIZON_DAYS, req.daysBefore()));
            managed.setReminderDaysBefore(d);
        }

        managed = userRepository.save(managed);
        return ReminderPreferenceResponse.from(managed);
    }

    // =============================================
    // Planificateur de rappels
    // =============================================

    /**
     * Chaque jour à 08h00 : envoie un rappel pour chaque tâche assignée dont
     * l'échéance tombe exactement dans `reminderDaysBefore` jours (déclenchement
     * unique grâce à la correspondance exacte sur le seuil).
     */
    @Scheduled(cron = "${app.reminders.cron:0 0 8 * * *}")
    @Transactional
    public void sendDueDateReminders() {
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(MAX_HORIZON_DAYS);

        List<Task> tasks = taskRepository.findRemindableTasks(today, horizon);
        for (Task task : tasks) {
            User assignee = task.getAssignee();
            if (assignee == null) continue;

            long daysUntil = ChronoUnit.DAYS.between(today, task.getDueDate());
            if (daysUntil != assignee.getReminderDaysBefore()) continue;
            if (!assignee.isReminderEmailEnabled() && !assignee.isReminderInAppEnabled()) continue;

            Long projectId = task.getProject() != null ? task.getProject().getId() : null;
            String dueLabel = dueLabel(daysUntil);
            String title = "Échéance proche : " + task.getTitle();
            String message = "La tâche " + task.getTaskKey() + " arrive à échéance " + dueLabel + ".";

            if (assignee.isReminderInAppEnabled()) {
                try {
                    notificationService.createNotification(
                            assignee, NotificationType.TASK_DUE_SOON,
                            title, message, projectId, task.getId(), null);
                } catch (Exception e) {
                    System.err.println("Rappel in-app échoué pour tâche " + task.getId() + ": " + e.getMessage());
                }
            }

            if (assignee.isReminderEmailEnabled() && assignee.getEmail() != null) {
                String link = frontendUrl + "/dashboard/projects/" + projectId + "/tasks/" + task.getId();
                String name = assignee.getNom() != null ? assignee.getNom() : assignee.getEmail();
                try {
                    emailService.sendTaskReminderEmail(
                            assignee.getEmail(), name, task.getTitle(), task.getTaskKey(), dueLabel, link);
                } catch (Exception e) {
                    System.err.println("Rappel email échoué pour tâche " + task.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    private String dueLabel(long daysUntil) {
        if (daysUntil <= 0) return "aujourd'hui";
        if (daysUntil == 1) return "demain";
        return "dans " + daysUntil + " jours";
    }
}
