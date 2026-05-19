package com.techwave.auth.collaboration.service;

import com.techwave.auth.collaboration.dto.AttachmentResponse;
import com.techwave.auth.collaboration.model.Attachment;
import com.techwave.auth.collaboration.repository.AttachmentRepository;
import com.techwave.auth.common.exception.ForbiddenException;
import com.techwave.auth.common.exception.ResourceNotFoundException;
import com.techwave.auth.project.model.ProjectRole;
import com.techwave.auth.project.model.Task;
import com.techwave.auth.project.repository.ProjectMemberRepository;
import com.techwave.auth.project.repository.TaskRepository;
import com.techwave.auth.user.model.User;
import com.techwave.auth.user.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository memberRepository;
    private final FileStorageService fileStorageService;
    private final JwtUtil jwtUtil;

    @Value("${app.backend.url:http://localhost:8082}")
    private String backendUrl;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             TaskRepository taskRepository,
                             ProjectMemberRepository memberRepository,
                             FileStorageService fileStorageService,
                             JwtUtil jwtUtil) {
        this.attachmentRepository = attachmentRepository;
        this.taskRepository = taskRepository;
        this.memberRepository = memberRepository;
        this.fileStorageService = fileStorageService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Upload d'un fichier joint à une tâche.
     */
    @Transactional
    public AttachmentResponse uploadAttachment(Long taskId, User user, MultipartFile file) {
        Task task = findActiveTask(taskId);
        checkMembership(task, user);

        String storedFileName = fileStorageService.storeFile(file);
        Path storagePath = fileStorageService.getFilePath(storedFileName);

        Attachment attachment = new Attachment();
        attachment.setOriginalFileName(file.getOriginalFilename());
        attachment.setStoredFileName(storedFileName);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setStoragePath(storagePath.toString());
        attachment.setTask(task);
        attachment.setUploadedBy(user);

        attachment = attachmentRepository.save(attachment);
        return AttachmentResponse.from(attachment);
    }

    /**
     * Liste des fichiers joints à une tâche.
     */
    public List<AttachmentResponse> getAttachments(Long taskId, User user) {
        Task task = findActiveTask(taskId);
        checkMembership(task, user);

        return AttachmentResponse.fromList(
                attachmentRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
        );
    }

    /**
     * Génère une URL signée (valable 1h) pour télécharger un fichier.
     * Retourne un map { "url": "...", "fileName": "...", "expiresIn": "1 heure" }.
     */
    public Map<String, String> getDownloadUrl(Long attachmentId, User user) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier non trouvé"));

        checkMembership(attachment.getTask(), user);

        // Générer un token signé pour le téléchargement (1 heure)
        String downloadToken = jwtUtil.generateToken(
                "download:" + attachmentId,
                List.of("DOWNLOAD")
        );

        String downloadUrl = backendUrl + "/api/attachments/file/" + downloadToken;

        return Map.of(
                "url", downloadUrl,
                "fileName", attachment.getOriginalFileName(),
                "expiresIn", "1 heure"
        );
    }

    /**
     * Servir le fichier à partir d'un token signé.
     * Appelé par le endpoint public /api/attachments/file/{token}.
     */
    public AttachmentDownload resolveDownloadToken(String token) {
        try {
            String subject = jwtUtil.extractUsername(token);
            if (subject == null || !subject.startsWith("download:")) {
                throw new ResourceNotFoundException("Lien de téléchargement invalide");
            }

            Long attachmentId = Long.parseLong(subject.substring("download:".length()));
            Attachment attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fichier non trouvé"));

            Path filePath = fileStorageService.getFilePath(attachment.getStoredFileName());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Le fichier n'est plus disponible");
            }

            return new AttachmentDownload(resource, attachment.getOriginalFileName(), attachment.getContentType());
        } catch (NumberFormatException | MalformedURLException e) {
            throw new ResourceNotFoundException("Lien de téléchargement invalide");
        } catch (Exception e) {
            if (e instanceof ResourceNotFoundException) throw e;
            throw new ResourceNotFoundException("Lien de téléchargement expiré ou invalide");
        }
    }

    /**
     * Supprimer un fichier joint (uploader ou admin/owner du projet).
     */
    @Transactional
    public void deleteAttachment(Long attachmentId, User user) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier non trouvé"));

        boolean isUploader = attachment.getUploadedBy().getId().equals(user.getId());
        boolean isProjectAdmin = memberRepository.findByProjectAndUser(attachment.getTask().getProject(), user)
                .map(m -> m.getRole() == ProjectRole.OWNER || m.getRole() == ProjectRole.ADMIN)
                .orElse(false);

        if (!isUploader && !isProjectAdmin) {
            throw new ForbiddenException("Vous ne pouvez supprimer que vos propres fichiers");
        }

        // Supprimer le fichier physique
        fileStorageService.deleteFile(attachment.getStoredFileName());

        // Supprimer l'entité
        attachmentRepository.delete(attachment);
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
     * Record interne pour le résultat d'un téléchargement.
     */
    public record AttachmentDownload(Resource resource, String fileName, String contentType) {}
}
