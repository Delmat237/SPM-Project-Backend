package com.techwave.auth.project.service;

import com.techwave.auth.common.exception.BusinessException;
import com.techwave.auth.common.exception.ForbiddenException;
import com.techwave.auth.common.exception.ResourceNotFoundException;
import com.techwave.auth.project.dto.*;
import com.techwave.auth.project.model.*;
import com.techwave.auth.project.repository.ProjectMemberRepository;
import com.techwave.auth.project.repository.ProjectRepository;
import com.techwave.auth.project.repository.TaskRepository;
import com.techwave.auth.user.model.User;
import com.techwave.auth.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final com.techwave.auth.websocket.service.WebSocketService webSocketService;

    // Labels français pour les colonnes Kanban
    private static final Map<TaskStatus, String> STATUS_LABELS = new LinkedHashMap<>();
    static {
        STATUS_LABELS.put(TaskStatus.TODO, "À faire");
        STATUS_LABELS.put(TaskStatus.IN_PROGRESS, "En cours");
        STATUS_LABELS.put(TaskStatus.IN_REVIEW, "En revue");
        STATUS_LABELS.put(TaskStatus.DONE, "Terminé");
        STATUS_LABELS.put(TaskStatus.BLOCKED, "Bloqué");
    }

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       ProjectMemberRepository memberRepository,
                       UserRepository userRepository,
                       com.techwave.auth.websocket.service.WebSocketService webSocketService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.webSocketService = webSocketService;
    }

    // =============================================
    // CRUD Tâches
    // =============================================

    /**
     * Liste paginée des tâches avec filtres optionnels.
     */
    public Page<TaskResponse> getTasks(Long projectId, User user,
                                       String status, Long assigneeId,
                                       String priority, Long parentId,
                                       int page, int size) {
        Project project = findActiveProject(projectId);
        checkMembership(project, user);

        // Parse des filtres optionnels
        TaskStatus statusFilter = parseEnum(status, TaskStatus.class, "Statut");
        TaskPriority priorityFilter = parseEnum(priority, TaskPriority.class, "Priorité");

        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("orderIndex").ascending().and(Sort.by("createdAt").descending()));

        return taskRepository.findTasks(projectId, statusFilter, assigneeId, priorityFilter, parentId, pageable)
                .map(TaskResponse::from);
    }

    /**
     * Créer une nouvelle tâche.
     */
    @Transactional
    public TaskResponse createTask(Long projectId, User user, CreateTaskRequest request) {
        Project project = findActiveProject(projectId);
        ProjectMember member = checkMembership(project, user);
        requireRole(member, ProjectRole.OWNER, ProjectRole.ADMIN, ProjectRole.MEMBER);

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setProject(project);
        task.setStatus(TaskStatus.TODO);

        // Priorité
        if (request.getPriority() != null) {
            task.setPriority(parseEnumRequired(request.getPriority(), TaskPriority.class,
                    "Priorité invalide. Valeurs acceptées : LOW, MEDIUM, HIGH, CRITICAL"));
        }

        // Assigné
        if (request.getAssigneeId() != null) {
            User assignee = findProjectMemberUser(project, request.getAssigneeId());
            task.setAssignee(assignee);
        }

        // Dates
        task.setStartDate(request.getStartDate());
        task.setDueDate(request.getDueDate());

        // Sous-tâche
        if (request.getParentId() != null) {
            Task parent = taskRepository.findActiveByIdAndProject(request.getParentId(), projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tâche parent non trouvée"));
            task.setParent(parent);
        }

        // Clé unique : PROJ_KEY-N
        long count = taskRepository.countByProject(projectId);
        task.setTaskKey(project.getProjectKey() + "-" + (count + 1));

        // Ordre : placer à la fin de la colonne TODO
        int maxOrder = taskRepository.findMaxOrderIndex(projectId, TaskStatus.TODO);
        task.setOrderIndex(maxOrder + 1);

        task = taskRepository.save(task);
        TaskResponse response = TaskResponse.from(task);
        try {
            webSocketService.sendProjectEvent(projectId, "task.created", response);
        } catch (Exception e) {}
        return response;
    }

    /**
     * Détail d'une tâche.
     */
    public TaskResponse getTask(Long projectId, Long taskId, User user) {
        Project project = findActiveProject(projectId);
        checkMembership(project, user);

        Task task = findActiveTask(projectId, taskId);
        return TaskResponse.from(task);
    }

    /**
     * Modifier une tâche (PATCH partiel).
     */
    @Transactional
    public TaskResponse updateTask(Long projectId, Long taskId, User user, UpdateTaskRequest request) {
        Project project = findActiveProject(projectId);
        ProjectMember member = checkMembership(project, user);
        requireRole(member, ProjectRole.OWNER, ProjectRole.ADMIN, ProjectRole.MEMBER);

        Task task = findActiveTask(projectId, taskId);

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(parseEnumRequired(request.getPriority(), TaskPriority.class,
                    "Priorité invalide. Valeurs acceptées : LOW, MEDIUM, HIGH, CRITICAL"));
        }
        if (request.getAssigneeId() != null) {
            User assignee = findProjectMemberUser(project, request.getAssigneeId());
            task.setAssignee(assignee);
        }
        if (request.getStartDate() != null) {
            task.setStartDate(request.getStartDate());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getOrderIndex() != null) {
            task.setOrderIndex(request.getOrderIndex());
        }

        task = taskRepository.save(task);
        TaskResponse response = TaskResponse.from(task);
        try {
            webSocketService.sendProjectEvent(projectId, "task.updated", response);
        } catch (Exception e) {}
        return response;
    }

    /**
     * Supprimer une tâche (soft delete). Archive les sous-tâches en cascade.
     */
    @Transactional
    public void deleteTask(Long projectId, Long taskId, User user) {
        Project project = findActiveProject(projectId);
        ProjectMember member = checkMembership(project, user);
        requireRole(member, ProjectRole.OWNER, ProjectRole.ADMIN, ProjectRole.MEMBER);

        Task task = findActiveTask(projectId, taskId);
        softDeleteRecursive(task);
        try {
            webSocketService.sendProjectEvent(projectId, "task.deleted", Map.of("taskId", taskId));
        } catch (Exception e) {}
    }

    /**
     * Changer le statut via la FSM.
     * @throws BusinessException (422) si la transition est interdite.
     */
    @Transactional
    public TaskResponse changeStatus(Long projectId, Long taskId, User user, ChangeStatusRequest request) {
        Project project = findActiveProject(projectId);
        ProjectMember member = checkMembership(project, user);
        requireRole(member, ProjectRole.OWNER, ProjectRole.ADMIN, ProjectRole.MEMBER);

        Task task = findActiveTask(projectId, taskId);

        TaskStatus newStatus = parseEnumRequired(request.getStatus(), TaskStatus.class,
                "Statut invalide. Valeurs acceptées : TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED");

        TaskStatus currentStatus = task.getStatus();
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new BusinessException(
                    String.format("Transition interdite : %s → %s. Transitions autorisées depuis %s : %s",
                            currentStatus, newStatus, currentStatus,
                            getAllowedTransitions(currentStatus))
            );
        }

        task.setStatus(newStatus);

        // Réordonner : placer à la fin de la nouvelle colonne
        int maxOrder = taskRepository.findMaxOrderIndex(projectId, newStatus);
        task.setOrderIndex(maxOrder + 1);

        task = taskRepository.save(task);
        TaskResponse response = TaskResponse.from(task);
        try {
            webSocketService.sendProjectEvent(projectId, "task.moved", Map.of(
                    "taskId", taskId,
                    "fromStatus", currentStatus.name(),
                    "toStatus", newStatus.name()
            ));
        } catch (Exception e) {}
        return response;
    }

    /**
     * Restaurer une tâche supprimée.
     */
    @Transactional
    public TaskResponse restoreTask(Long projectId, Long taskId, User user) {
        Project project = findActiveProject(projectId);
        ProjectMember member = checkMembership(project, user);
        requireRole(member, ProjectRole.OWNER, ProjectRole.ADMIN);

        Task task = taskRepository.findByIdAndProject(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée"));

        if (task.getDeletedAt() == null) {
            throw new BusinessException("Cette tâche n'est pas dans la corbeille");
        }

        task.setDeletedAt(null);
        task = taskRepository.save(task);
        TaskResponse response = TaskResponse.from(task);
        try {
            webSocketService.sendProjectEvent(projectId, "task.updated", response);
        } catch (Exception e) {}
        return response;
    }

    // =============================================
    // Vues spéciales
    // =============================================

    /**
     * Vue Kanban : tâches groupées par statut.
     */
    public KanbanResponse getKanbanView(Long projectId, User user) {
        Project project = findActiveProject(projectId);
        checkMembership(project, user);

        List<Task> allTasks = taskRepository.findAllActiveTasks(projectId);

        // Grouper par statut
        Map<TaskStatus, List<Task>> grouped = allTasks.stream()
                .collect(Collectors.groupingBy(Task::getStatus));

        // Construire les colonnes dans l'ordre défini
        List<KanbanResponse.KanbanColumn> columns = new ArrayList<>();
        for (Map.Entry<TaskStatus, String> entry : STATUS_LABELS.entrySet()) {
            TaskStatus status = entry.getKey();
            String label = entry.getValue();
            List<Task> columnTasks = grouped.getOrDefault(status, List.of());

            columns.add(KanbanResponse.KanbanColumn.builder()
                    .id(status.name())
                    .name(label)
                    .tasks(TaskResponse.fromList(columnTasks))
                    .build());
        }

        return KanbanResponse.builder().columns(columns).build();
    }

    /**
     * Vue Gantt : tâches avec dates et dépendances.
     */
    public GanttResponse getGanttView(Long projectId, User user) {
        Project project = findActiveProject(projectId);
        checkMembership(project, user);

        List<Task> allTasks = taskRepository.findAllActiveTasks(projectId);
        return GanttResponse.from(allTasks);
    }

    // =============================================
    // Sous-tâches
    // =============================================

    /**
     * Liste des sous-tâches d'une tâche.
     */
    public List<TaskResponse> getSubtasks(Long projectId, Long taskId, User user) {
        Project project = findActiveProject(projectId);
        checkMembership(project, user);

        // Vérifier que la tâche parent existe
        findActiveTask(projectId, taskId);

        List<Task> subtasks = taskRepository.findActiveSubtasks(taskId);
        return TaskResponse.fromList(subtasks);
    }

    /**
     * Créer une sous-tâche (raccourci pour POST /tasks/{id}/subtasks).
     */
    @Transactional
    public TaskResponse createSubtask(Long projectId, Long parentId, User user, CreateTaskRequest request) {
        // Forcer le parentId
        request.setParentId(parentId);
        return createTask(projectId, user, request);
    }

    // =============================================
    // Méthodes utilitaires
    // =============================================

    private Project findActiveProject(Long id) {
        return projectRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));
    }

    private ProjectMember checkMembership(Project project, User user) {
        return memberRepository.findByProjectAndUser(project, user)
                .orElseThrow(() -> new ForbiddenException("Vous n'êtes pas membre de ce projet"));
    }

    private void requireRole(ProjectMember member, ProjectRole... allowedRoles) {
        for (ProjectRole role : allowedRoles) {
            if (member.getRole() == role) return;
        }
        throw new ForbiddenException("Droits insuffisants pour cette action");
    }

    private Task findActiveTask(Long projectId, Long taskId) {
        return taskRepository.findActiveByIdAndProject(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée"));
    }

    private User findProjectMemberUser(Project project, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        if (!memberRepository.existsByProjectAndUser(project, user)) {
            throw new BusinessException("L'utilisateur assigné doit être membre du projet");
        }
        return user;
    }

    /**
     * Soft delete récursif : supprime la tâche et toutes ses sous-tâches.
     */
    private void softDeleteRecursive(Task task) {
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);

        List<Task> children = taskRepository.findAllSubtasks(task.getId());
        for (Task child : children) {
            if (child.getDeletedAt() == null) {
                softDeleteRecursive(child);
            }
        }
    }

    /**
     * Parse un enum de manière optionnelle (retourne null si la valeur est null ou vide).
     */
    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumClass, String fieldName) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    fieldName + " invalide : '" + value + "'. Valeurs acceptées : " +
                            Arrays.toString(enumClass.getEnumConstants()));
        }
    }

    /**
     * Parse un enum de manière obligatoire (lève une exception si la valeur est invalide).
     */
    private <E extends Enum<E>> E parseEnumRequired(String value, Class<E> enumClass, String errorMessage) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Retourne la liste des transitions autorisées depuis un statut donné.
     */
    private String getAllowedTransitions(TaskStatus status) {
        return Arrays.stream(TaskStatus.values())
                .filter(status::canTransitionTo)
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
