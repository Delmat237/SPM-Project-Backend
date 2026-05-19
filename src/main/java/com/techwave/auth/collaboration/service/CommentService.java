package com.techwave.auth.collaboration.service;

import com.techwave.auth.collaboration.dto.CommentResponse;
import com.techwave.auth.collaboration.dto.CreateCommentRequest;
import com.techwave.auth.collaboration.dto.UpdateCommentRequest;
import com.techwave.auth.collaboration.model.Comment;
import com.techwave.auth.collaboration.model.NotificationType;
import com.techwave.auth.collaboration.repository.CommentRepository;
import com.techwave.auth.common.exception.ForbiddenException;
import com.techwave.auth.common.exception.ResourceNotFoundException;
import com.techwave.auth.project.model.Task;
import com.techwave.auth.project.repository.ProjectMemberRepository;
import com.techwave.auth.project.repository.TaskRepository;
import com.techwave.auth.user.model.User;
import com.techwave.auth.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final com.techwave.auth.websocket.service.WebSocketService webSocketService;

    /**
     * Regex pour détecter les @mentions.
     * Supporte : @user@email.com et @nom (mot simple).
     */
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,})");

    public CommentService(CommentRepository commentRepository,
                          TaskRepository taskRepository,
                          ProjectMemberRepository memberRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          com.techwave.auth.websocket.service.WebSocketService webSocketService) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.webSocketService = webSocketService;
    }

    /**
     * Liste des commentaires d'une tâche.
     */
    public List<CommentResponse> getComments(Long taskId, User user) {
        Task task = findActiveTask(taskId);
        checkMembership(task, user);

        List<Comment> comments = commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        return CommentResponse.fromList(comments);
    }

    /**
     * Ajouter un commentaire avec détection des @mentions.
     */
    @Transactional
    public CommentResponse createComment(Long taskId, User user, CreateCommentRequest request) {
        Task task = findActiveTask(taskId);
        checkMembership(task, user);

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setTask(task);
        comment.setAuthor(user);
        final Comment savedComment = commentRepository.save(comment);

        // Notifier l'assigné de la tâche (s'il existe et n'est pas l'auteur)
        if (task.getAssignee() != null && !task.getAssignee().getId().equals(user.getId())) {
            notificationService.createNotification(
                    task.getAssignee(),
                    NotificationType.COMMENT_ADDED,
                    "Nouveau commentaire sur " + task.getTaskKey(),
                    user.getNom() + " a commenté la tâche \"" + task.getTitle() + "\"",
                    task.getProject().getId(),
                    task.getId(),
                    savedComment.getId()
            );
        }

        // Parser les @mentions et notifier
        Set<String> mentionedEmails = parseMentions(request.getContent());
        for (String email : mentionedEmails) {
            userRepository.findByEmail(email).ifPresent(mentionedUser -> {
                // Ne pas notifier l'auteur lui-même, ni l'assigné déjà notifié
                if (!mentionedUser.getId().equals(user.getId()) &&
                        (task.getAssignee() == null || !mentionedUser.getId().equals(task.getAssignee().getId()))) {
                    notificationService.createNotification(
                            mentionedUser,
                            NotificationType.MENTION,
                            "Vous êtes mentionné dans " + task.getTaskKey(),
                            user.getNom() + " vous a mentionné dans un commentaire sur \"" + task.getTitle() + "\"",
                            task.getProject().getId(),
                            task.getId(),
                            savedComment.getId()
                    );
                }
            });
        }

        CommentResponse response = CommentResponse.from(savedComment);
        try {
            webSocketService.sendCommentEvent(taskId, "comment.created", response);
        } catch (Exception e) {
            // Ignorer l'erreur WebSocket
        }

        return response;
    }

    /**
     * Modifier un commentaire (auteur uniquement).
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, User user, UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire non trouvé"));

        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenException("Vous ne pouvez modifier que vos propres commentaires");
        }

        comment.setContent(request.getContent());
        comment = commentRepository.save(comment);

        CommentResponse response = CommentResponse.from(comment);
        try {
            webSocketService.sendCommentEvent(comment.getTask().getId(), "comment.updated", response);
        } catch (Exception e) {
            // Ignorer
        }

        return response;
    }

    /**
     * Supprimer un commentaire (auteur ou ADMIN/OWNER du projet).
     */
    @Transactional
    public void deleteComment(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire non trouvé"));

        boolean isAuthor = comment.getAuthor().getId().equals(user.getId());
        boolean isProjectAdmin = memberRepository.findByProjectAndUser(comment.getTask().getProject(), user)
                .map(m -> m.getRole() == com.techwave.auth.project.model.ProjectRole.OWNER
                        || m.getRole() == com.techwave.auth.project.model.ProjectRole.ADMIN)
                .orElse(false);

        if (!isAuthor && !isProjectAdmin) {
            throw new ForbiddenException("Vous ne pouvez supprimer que vos propres commentaires");
        }

        Long taskId = comment.getTask().getId();
        commentRepository.delete(comment);

        try {
            webSocketService.sendCommentEvent(taskId, "comment.deleted", java.util.Map.of("commentId", commentId));
        } catch (Exception e) {
            // Ignorer
        }
    }

    // =============================================
    // Utilitaires
    // =============================================

    private Task findActiveTask(Long taskId) {
        return taskRepository.findById(taskId)
                .filter(t -> t.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée"));
    }

    private void checkMembership(Task task, User user) {
        if (!memberRepository.existsByProjectAndUser(task.getProject(), user)) {
            throw new ForbiddenException("Vous n'êtes pas membre de ce projet");
        }
    }

    /**
     * Extrait les emails mentionnés dans le contenu (@email@domain.com).
     */
    private Set<String> parseMentions(String content) {
        Set<String> emails = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            emails.add(matcher.group(1).toLowerCase());
        }
        return emails;
    }
}
