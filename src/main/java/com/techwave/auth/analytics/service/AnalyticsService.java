package com.techwave.auth.analytics.service;

import com.techwave.auth.analytics.dto.BurndownResponse;
import com.techwave.auth.analytics.dto.ProjectSummaryResponse;
import com.techwave.auth.analytics.dto.VelocityResponse;
import com.techwave.auth.common.exception.ForbiddenException;
import com.techwave.auth.common.exception.ResourceNotFoundException;
import com.techwave.auth.project.model.Project;
import com.techwave.auth.project.model.TaskStatus;
import com.techwave.auth.project.repository.ProjectMemberRepository;
import com.techwave.auth.project.repository.ProjectRepository;
import com.techwave.auth.project.repository.TaskRepository;
import com.techwave.auth.user.model.User;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AnalyticsService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;

    public AnalyticsService(TaskRepository taskRepository,
                            ProjectRepository projectRepository,
                            ProjectMemberRepository memberRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Résumé analytique d'un projet : compteurs par statut, retards, taux de complétion.
     */
    public ProjectSummaryResponse getSummary(Long projectId, User user) {
        Project project = findActiveProject(projectId);
        checkMembership(project, user);

        long todo = taskRepository.countByProjectAndStatus(projectId, TaskStatus.TODO);
        long inProgress = taskRepository.countByProjectAndStatus(projectId, TaskStatus.IN_PROGRESS);
        long inReview = taskRepository.countByProjectAndStatus(projectId, TaskStatus.IN_REVIEW);
        long done = taskRepository.countByProjectAndStatus(projectId, TaskStatus.DONE);
        long blocked = taskRepository.countByProjectAndStatus(projectId, TaskStatus.BLOCKED);
        long total = todo + inProgress + inReview + done + blocked;
        long overdue = taskRepository.countOverdueTasks(projectId, LocalDate.now());

        double completionRate = total > 0 ? Math.round((double) done / total * 1000.0) / 10.0 : 0.0;

        int memberCount = memberRepository.countByProject(project);

        return ProjectSummaryResponse.builder()
                .totalTasks(total)
                .completedTasks(done)
                .inProgressTasks(inProgress)
                .todoTasks(todo)
                .blockedTasks(blocked)
                .inReviewTasks(inReview)
                .overdueCount(overdue)
                .completionRate(completionRate)
                .memberCount(memberCount)
                .build();
    }

    /**
     * Burndown chart : nombre de tâches restantes par jour sur la période d'un sprint.
     */
    public BurndownResponse getBurndown(Long projectId, User user, LocalDate sprintStart, LocalDate sprintEnd) {
        Project project = findActiveProject(projectId);
        checkMembership(project, user);

        if (sprintEnd.isBefore(sprintStart)) {
            throw new IllegalArgumentException("sprintEnd doit être après sprintStart");
        }

        // Nombre total de tâches existantes au début du sprint
        LocalDateTime sprintStartDateTime = sprintStart.atStartOfDay();
        long totalAtStart = taskRepository.countTasksCreatedBefore(projectId, sprintStartDateTime);

        List<BurndownResponse.BurndownDataPoint> dataPoints = new ArrayList<>();
        LocalDate currentDate = sprintStart;

        while (!currentDate.isAfter(sprintEnd) && !currentDate.isAfter(LocalDate.now())) {
            LocalDateTime endOfDay = currentDate.atTime(LocalTime.MAX);

            // Tâches totales créées jusqu'à ce jour
            long totalCreated = taskRepository.countTasksCreatedBefore(projectId, endOfDay);
            // Tâches complétées jusqu'à ce jour
            long completedSoFar = taskRepository.countCompletedTasksBefore(projectId, endOfDay);
            // Restant = total créées - complétées
            long remaining = totalCreated - completedSoFar;

            dataPoints.add(BurndownResponse.BurndownDataPoint.builder()
                    .date(currentDate)
                    .remaining(Math.max(0, remaining))
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return BurndownResponse.builder()
                .sprintStart(sprintStart)
                .sprintEnd(sprintEnd)
                .totalTasks(totalAtStart)
                .dataPoints(dataPoints)
                .build();
    }

    /**
     * Vélocité de l'équipe : tâches complétées par semaine sur les 12 dernières semaines.
     */
    public VelocityResponse getVelocity(Long projectId, User user) {
        Project project = findActiveProject(projectId);
        checkMembership(project, user);

        int weeksToAnalyze = 12;
        List<VelocityResponse.VelocityDataPoint> sprints = new ArrayList<>();
        long totalCompleted = 0;

        LocalDate today = LocalDate.now();

        for (int i = weeksToAnalyze - 1; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);

            LocalDateTime start = weekStart.atStartOfDay();
            LocalDateTime end = weekEnd.plusDays(1).atStartOfDay(); // exclusive

            long completed = taskRepository.countCompletedTasksBetween(projectId, start, end);
            totalCompleted += completed;

            int weekNumber = weekStart.get(WeekFields.of(Locale.FRANCE).weekOfYear());

            sprints.add(VelocityResponse.VelocityDataPoint.builder()
                    .label("Semaine " + weekNumber)
                    .startDate(weekStart)
                    .endDate(weekEnd)
                    .completedTasks(completed)
                    .build());
        }

        double averageVelocity = weeksToAnalyze > 0
                ? Math.round((double) totalCompleted / weeksToAnalyze * 10.0) / 10.0
                : 0.0;

        return VelocityResponse.builder()
                .sprints(sprints)
                .averageVelocity(averageVelocity)
                .build();
    }

    // =============================================
    // Utilitaires
    // =============================================

    private Project findActiveProject(Long id) {
        return projectRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));
    }

    private void checkMembership(Project project, User user) {
        if (!memberRepository.existsByProjectAndUser(project, user)) {
            throw new ForbiddenException("Vous n'êtes pas membre de ce projet");
        }
    }
}
