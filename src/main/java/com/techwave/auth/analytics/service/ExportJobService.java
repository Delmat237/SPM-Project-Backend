package com.techwave.auth.analytics.service;

import com.techwave.auth.analytics.dto.ExportJobResponse;
import com.techwave.auth.analytics.model.ExportJob;
import com.techwave.auth.analytics.model.ExportStatus;
import com.techwave.auth.analytics.repository.ExportJobRepository;
import com.techwave.auth.common.exception.BusinessException;
import com.techwave.auth.common.exception.ForbiddenException;
import com.techwave.auth.common.exception.ResourceNotFoundException;
import com.techwave.auth.project.model.Project;
import com.techwave.auth.project.model.Task;
import com.techwave.auth.project.repository.ProjectMemberRepository;
import com.techwave.auth.project.repository.ProjectRepository;
import com.techwave.auth.project.repository.TaskRepository;
import com.techwave.auth.user.model.User;
import com.techwave.auth.user.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExportJobService {

    private final ExportJobRepository exportJobRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final TaskRepository taskRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.backend.url:http://localhost:8082}")
    private String backendUrl;

    public ExportJobService(ExportJobRepository exportJobRepository,
                            ProjectRepository projectRepository,
                            ProjectMemberRepository memberRepository,
                            TaskRepository taskRepository,
                            JwtUtil jwtUtil) {
        this.exportJobRepository = exportJobRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.taskRepository = taskRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Crée un job d'export et lance le traitement en arrière-plan.
     */
    @Transactional
    public ExportJobResponse createExportJob(Long projectId, String format, User user) {
        Project project = findActiveProject(projectId);
        checkMembership(project, user);

        // Valider le format
        String normalizedFormat = format != null ? format.toUpperCase() : "CSV";
        if (!normalizedFormat.equals("CSV") && !normalizedFormat.equals("JSON")) {
            throw new BusinessException("Format d'export invalide. Formats acceptés : CSV, JSON");
        }

        ExportJob job = new ExportJob();
        job.setProjectId(projectId);
        job.setFormat(normalizedFormat);
        job.setStatus(ExportStatus.PENDING);
        job.setRequestedBy(user);
        job = exportJobRepository.save(job);

        // Lancer le traitement asynchrone
        processExportAsync(job.getId());

        return ExportJobResponse.from(job);
    }

    /**
     * Récupérer le statut d'un job d'export.
     */
    public ExportJobResponse getExportJob(Long jobId, User user) {
        ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job d'export non trouvé"));

        if (!job.getRequestedBy().getId().equals(user.getId())) {
            throw new ForbiddenException("Vous n'avez pas accès à ce job d'export");
        }

        return ExportJobResponse.from(job);
    }

    /**
     * Génère l'URL de téléchargement pour un export terminé.
     * Retourne une URL signée (302) ou une erreur si le job n'est pas terminé.
     */
    public String getDownloadUrl(Long jobId, User user) {
        ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job d'export non trouvé"));

        if (!job.getRequestedBy().getId().equals(user.getId())) {
            throw new ForbiddenException("Vous n'avez pas accès à ce job d'export");
        }

        if (job.getStatus() != ExportStatus.DONE) {
            throw new BusinessException("L'export n'est pas encore terminé. Statut actuel : " + job.getStatus());
        }

        if (job.getExpiresAt() != null && job.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Ce fichier d'export a expiré");
        }

        // Générer un token signé pour le téléchargement
        String downloadToken = jwtUtil.generateToken(
                "export:" + jobId,
                List.of("DOWNLOAD")
        );

        return backendUrl + "/api/exports/file/" + downloadToken;
    }

    /**
     * Résoudre un token de téléchargement et retourner le chemin du fichier.
     */
    public ExportDownload resolveDownloadToken(String token) {
        try {
            String subject = jwtUtil.extractUsername(token);
            if (subject == null || !subject.startsWith("export:")) {
                throw new ResourceNotFoundException("Lien de téléchargement invalide");
            }

            Long jobId = Long.parseLong(subject.substring("export:".length()));
            ExportJob job = exportJobRepository.findById(jobId)
                    .orElseThrow(() -> new ResourceNotFoundException("Export non trouvé"));

            if (job.getStatus() != ExportStatus.DONE || job.getStoredFileName() == null) {
                throw new ResourceNotFoundException("Le fichier d'export n'est pas disponible");
            }

            Path filePath = Paths.get(uploadDir, "exports", job.getStoredFileName());
            if (!Files.exists(filePath)) {
                throw new ResourceNotFoundException("Le fichier d'export a été supprimé");
            }

            String contentType = job.getFormat().equals("CSV") ? "text/csv" : "application/json";
            String fileName = "export_project_" + job.getProjectId() + "." + job.getFormat().toLowerCase();

            return new ExportDownload(filePath, fileName, contentType);
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Lien de téléchargement invalide");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceNotFoundException("Lien de téléchargement expiré ou invalide");
        }
    }

    // =============================================
    // Traitement asynchrone
    // =============================================

    @Async("exportExecutor")
    public void processExportAsync(Long jobId) {
        ExportJob job = exportJobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        try {
            job.setStatus(ExportStatus.PROCESSING);
            exportJobRepository.save(job);

            // Récupérer les tâches du projet
            List<Task> tasks = taskRepository.findAllActiveTasks(job.getProjectId());
            Project project = projectRepository.findById(job.getProjectId()).orElse(null);
            if (project == null) {
                throw new RuntimeException("Projet non trouvé");
            }

            // Générer le contenu
            String content;
            if ("CSV".equals(job.getFormat())) {
                content = generateCsv(tasks, project);
            } else {
                content = generateJson(tasks, project);
            }

            // Sauvegarder le fichier
            String extension = job.getFormat().equalsIgnoreCase("CSV") ? ".csv" : ".json";
            String storedFileName = UUID.randomUUID().toString() + extension;
            Path exportDir = Paths.get(uploadDir, "exports");
            Files.createDirectories(exportDir);
            Path filePath = exportDir.resolve(storedFileName);
            Files.writeString(filePath, content);

            // Mettre à jour le job
            job.setStatus(ExportStatus.DONE);
            job.setStoredFileName(storedFileName);
            job.setFilePath(filePath.toString());
            job.setCompletedAt(LocalDateTime.now());
            job.setExpiresAt(LocalDateTime.now().plusHours(24));
            exportJobRepository.save(job);

        } catch (Exception e) {
            job.setStatus(ExportStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            exportJobRepository.save(job);
        }
    }

    // =============================================
    // Générateurs de fichiers
    // =============================================

    private String generateCsv(List<Task> tasks, Project project) {
        StringBuilder csv = new StringBuilder();
        csv.append("Projet: ").append(project.getName()).append("\n");
        csv.append("Clé,Titre,Statut,Priorité,Assigné,Date début,Date fin,Créé le\n");

        for (Task task : tasks) {
            csv.append(escapeCsv(task.getTaskKey())).append(",");
            csv.append(escapeCsv(task.getTitle())).append(",");
            csv.append(task.getStatus().name()).append(",");
            csv.append(task.getPriority().name()).append(",");
            csv.append(escapeCsv(task.getAssignee() != null ? task.getAssignee().getNom() : "Non assigné")).append(",");
            csv.append(task.getStartDate() != null ? task.getStartDate() : "").append(",");
            csv.append(task.getDueDate() != null ? task.getDueDate() : "").append(",");
            csv.append(task.getCreatedAt()).append("\n");
        }

        return csv.toString();
    }

    private String generateJson(List<Task> tasks, Project project) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"project\": \"").append(escapeJson(project.getName())).append("\",\n");
        json.append("  \"projectKey\": \"").append(escapeJson(project.getProjectKey())).append("\",\n");
        json.append("  \"exportedAt\": \"").append(LocalDateTime.now()).append("\",\n");
        json.append("  \"totalTasks\": ").append(tasks.size()).append(",\n");
        json.append("  \"tasks\": [\n");

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            json.append("    {\n");
            json.append("      \"key\": \"").append(escapeJson(task.getTaskKey())).append("\",\n");
            json.append("      \"title\": \"").append(escapeJson(task.getTitle())).append("\",\n");
            json.append("      \"description\": ").append(task.getDescription() != null
                    ? "\"" + escapeJson(task.getDescription()) + "\"" : "null").append(",\n");
            json.append("      \"status\": \"").append(task.getStatus().name()).append("\",\n");
            json.append("      \"priority\": \"").append(task.getPriority().name()).append("\",\n");
            json.append("      \"assignee\": ").append(task.getAssignee() != null
                    ? "\"" + escapeJson(task.getAssignee().getNom()) + "\"" : "null").append(",\n");
            json.append("      \"startDate\": ").append(task.getStartDate() != null
                    ? "\"" + task.getStartDate() + "\"" : "null").append(",\n");
            json.append("      \"dueDate\": ").append(task.getDueDate() != null
                    ? "\"" + task.getDueDate() + "\"" : "null").append(",\n");
            json.append("      \"createdAt\": \"").append(task.getCreatedAt()).append("\"\n");
            json.append("    }").append(i < tasks.size() - 1 ? "," : "").append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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

    public record ExportDownload(Path filePath, String fileName, String contentType) {}
}
