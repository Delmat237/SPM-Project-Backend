package com.techwave.auth.project.service;

import com.techwave.auth.common.exception.BusinessException;
import com.techwave.auth.common.exception.ForbiddenException;
import com.techwave.auth.common.exception.ResourceNotFoundException;
import com.techwave.auth.project.dto.*;
import com.techwave.auth.project.model.*;
import com.techwave.auth.project.repository.InvitationRepository;
import com.techwave.auth.project.repository.ProjectMemberRepository;
import com.techwave.auth.project.repository.ProjectRepository;
import com.techwave.auth.user.model.User;
import com.techwave.auth.user.repository.UserRepository;
import com.techwave.auth.user.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final com.techwave.auth.websocket.service.WebSocketService webSocketService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMemberRepository memberRepository,
                          InvitationRepository invitationRepository,
                          UserRepository userRepository,
                          EmailService emailService,
                          com.techwave.auth.websocket.service.WebSocketService webSocketService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.webSocketService = webSocketService;
    }

    // =============================================
    // CRUD Projets
    // =============================================

    public Page<ProjectResponse> getMyProjects(User user, Pageable pageable) {
        return projectRepository.findMyProjects(user, pageable)
                .map(project -> {
                    ProjectMember member = memberRepository.findByProjectAndUser(project, user).orElse(null);
                    return ProjectResponse.from(project, member);
                });
    }

    @Transactional
    public ProjectResponse createProject(User user, CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setProjectKey(generateProjectKey(request.getName()));
        project.setOwner(user);

        if (request.getVisibility() != null) {
            try {
                project.setVisibility(ProjectVisibility.valueOf(request.getVisibility().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Visibilité invalide. Valeurs acceptées : PUBLIC, PRIVATE");
            }
        }

        project = projectRepository.save(project);

        // Le créateur devient membre avec le rôle OWNER
        ProjectMember ownerMember = new ProjectMember();
        ownerMember.setProject(project);
        ownerMember.setUser(user);
        ownerMember.setRole(ProjectRole.OWNER);
        memberRepository.save(ownerMember);

        project.getMembers().add(ownerMember);

        return ProjectResponse.from(project, ownerMember);
    }

    public ProjectResponse getProject(Long id, User user) {
        Project project = findActiveProject(id);
        ProjectMember member = checkMembership(project, user);
        return ProjectResponse.from(project, member);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, User user, UpdateProjectRequest request) {
        Project project = findActiveProject(id);
        ProjectMember member = checkMembership(project, user);
        requireRole(member, ProjectRole.OWNER, ProjectRole.ADMIN, ProjectRole.MEMBER);

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getVisibility() != null) {
            requireRole(member, ProjectRole.OWNER, ProjectRole.ADMIN);
            try {
                project.setVisibility(ProjectVisibility.valueOf(request.getVisibility().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Visibilité invalide. Valeurs acceptées : PUBLIC, PRIVATE");
            }
        }

        project = projectRepository.save(project);
        return ProjectResponse.from(project, member);
    }

    @Transactional
    public void deleteProject(Long id, User user) {
        Project project = findActiveProject(id);
        ProjectMember member = checkMembership(project, user);
        requireRole(member, ProjectRole.OWNER);

        // Soft delete : marque la date de suppression (corbeille 30 jours)
        project.setDeletedAt(LocalDateTime.now());
        projectRepository.save(project);
    }

    @Transactional
    public ProjectResponse archiveProject(Long id, User user, ArchiveRequest request) {
        Project project = findActiveProject(id);
        ProjectMember member = checkMembership(project, user);
        requireRole(member, ProjectRole.OWNER, ProjectRole.ADMIN);

        project.setArchived(request.isArchived());
        project = projectRepository.save(project);
        return ProjectResponse.from(project, member);
    }

    // =============================================
    // Gestion des Membres
    // =============================================

    public List<MemberResponse> getMembers(Long projectId, User user) {
        Project project = findActiveProject(projectId);
        checkMembership(project, user);

        return memberRepository.findByProject(project).stream()
                .map(MemberResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public InvitationResponse addMember(Long projectId, User currentUser, AddMemberRequest request) {
        Project project = findActiveProject(projectId);
        ProjectMember currentMember = checkMembership(project, currentUser);
        requireRole(currentMember, ProjectRole.OWNER, ProjectRole.ADMIN);

        // Valider le rôle demandé
        ProjectRole requestedRole;
        try {
            requestedRole = ProjectRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rôle invalide. Valeurs acceptées : ADMIN, MEMBER, READER");
        }

        if (requestedRole == ProjectRole.OWNER) {
            throw new BusinessException("Impossible d'inviter quelqu'un en tant que OWNER");
        }

        // Vérifier si l'utilisateur est déjà membre
        userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
            if (memberRepository.existsByProjectAndUser(project, existingUser)) {
                throw new BusinessException("Cet utilisateur est déjà membre du projet");
            }
        });

        // Vérifier si une invitation est déjà en attente
        if (invitationRepository.existsByProjectAndEmailAndStatus(project, request.getEmail(), InvitationStatus.PENDING)) {
            throw new BusinessException("Une invitation est déjà en attente pour cet email");
        }

        // Créer l'invitation
        Invitation invitation = new Invitation();
        invitation.setProject(project);
        invitation.setEmail(request.getEmail());
        invitation.setRole(requestedRole);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setInvitedBy(currentUser);
        invitation.setExpiryDate(LocalDateTime.now().plusDays(7));
        invitation = invitationRepository.save(invitation);

        // Envoyer l'email d'invitation
        String inviteLink = frontendUrl + "/invitations/" + invitation.getToken();
        emailService.sendProjectInvitationEmail(
                request.getEmail(),
                currentUser.getNom(),
                project.getName(),
                requestedRole.name(),
                inviteLink
        );

        return InvitationResponse.from(invitation);
    }

    @Transactional
    public MemberResponse updateMemberRole(Long projectId, Long userId, User currentUser, UpdateMemberRoleRequest request) {
        Project project = findActiveProject(projectId);
        ProjectMember currentMember = checkMembership(project, currentUser);
        requireRole(currentMember, ProjectRole.OWNER, ProjectRole.ADMIN);

        ProjectMember targetMember = memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membre non trouvé dans ce projet"));

        // Empêcher la modification du rôle OWNER
        if (targetMember.getRole() == ProjectRole.OWNER) {
            throw new BusinessException("Impossible de modifier le rôle du propriétaire");
        }

        // Seul l'OWNER peut promouvoir en ADMIN
        ProjectRole newRole;
        try {
            newRole = ProjectRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rôle invalide. Valeurs acceptées : ADMIN, MEMBER, READER");
        }

        if (newRole == ProjectRole.OWNER) {
            throw new BusinessException("Impossible de transférer le rôle OWNER via cette route");
        }

        if (newRole == ProjectRole.ADMIN && currentMember.getRole() != ProjectRole.OWNER) {
            throw new ForbiddenException("Seul le propriétaire peut promouvoir en ADMIN");
        }

        targetMember.setRole(newRole);
        targetMember = memberRepository.save(targetMember);
        return MemberResponse.from(targetMember);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, User currentUser) {
        Project project = findActiveProject(projectId);
        ProjectMember currentMember = checkMembership(project, currentUser);
        requireRole(currentMember, ProjectRole.OWNER, ProjectRole.ADMIN);

        ProjectMember targetMember = memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membre non trouvé dans ce projet"));

        if (targetMember.getRole() == ProjectRole.OWNER) {
            throw new BusinessException("Impossible de retirer le propriétaire du projet");
        }

        // Un ADMIN ne peut pas retirer un autre ADMIN (seul l'OWNER peut)
        if (targetMember.getRole() == ProjectRole.ADMIN && currentMember.getRole() != ProjectRole.OWNER) {
            throw new ForbiddenException("Seul le propriétaire peut retirer un administrateur");
        }

        memberRepository.delete(targetMember);
        try {
            webSocketService.sendProjectEvent(projectId, "member.removed", java.util.Map.of("userId", userId));
        } catch (Exception e) {}
    }

    // =============================================
    // Export
    // =============================================

    public byte[] exportProject(Long id, User user, ExportRequest request) {
        Project project = findActiveProject(id);
        checkMembership(project, user);

        List<ProjectMember> members = memberRepository.findByProject(project);
        String format = request.getFormat().toUpperCase();

        switch (format) {
            case "CSV":
                return generateCsvExport(project, members);
            case "JSON":
                return generateJsonExport(project, members);
            default:
                throw new BusinessException("Format d'export non supporté : " + format + ". Formats acceptés : CSV, JSON");
        }
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
            if (member.getRole() == role) {
                return;
            }
        }
        throw new ForbiddenException("Droits insuffisants pour cette action");
    }

    private String generateProjectKey(String name) {
        // Prend les initiales de chaque mot, max 5 caractères
        String[] words = name.trim().split("\\s+");
        StringBuilder key = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty() && key.length() < 5) {
                key.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        // Si la clé est trop courte, compléter avec les premières lettres du nom
        if (key.length() < 2) {
            key = new StringBuilder(name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase());
            if (key.length() > 5) key = new StringBuilder(key.substring(0, 5));
        }

        String baseKey = key.toString();
        String finalKey = baseKey;
        int counter = 1;
        while (projectRepository.existsByProjectKey(finalKey)) {
            finalKey = baseKey + counter;
            counter++;
        }
        return finalKey;
    }

    private byte[] generateCsvExport(Project project, List<ProjectMember> members) {
        StringBuilder csv = new StringBuilder();
        csv.append("Projet,").append(project.getName()).append("\n");
        csv.append("Clé,").append(project.getProjectKey()).append("\n");
        csv.append("Description,\"").append(project.getDescription() != null ? project.getDescription() : "").append("\"\n");
        csv.append("Visibilité,").append(project.getVisibility()).append("\n");
        csv.append("Archivé,").append(project.isArchived()).append("\n");
        csv.append("Créé le,").append(project.getCreatedAt()).append("\n\n");
        csv.append("--- Membres ---\n");
        csv.append("Email,Nom,Rôle,Rejoint le\n");
        for (ProjectMember m : members) {
            csv.append(m.getUser().getEmail()).append(",")
                    .append(m.getUser().getNom()).append(",")
                    .append(m.getRole()).append(",")
                    .append(m.getJoinedAt()).append("\n");
        }
        return csv.toString().getBytes();
    }

    private byte[] generateJsonExport(Project project, List<ProjectMember> members) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"project\": {\n");
        json.append("    \"id\": ").append(project.getId()).append(",\n");
        json.append("    \"name\": \"").append(project.getName()).append("\",\n");
        json.append("    \"key\": \"").append(project.getProjectKey()).append("\",\n");
        json.append("    \"description\": \"").append(project.getDescription() != null ? project.getDescription() : "").append("\",\n");
        json.append("    \"visibility\": \"").append(project.getVisibility()).append("\",\n");
        json.append("    \"archived\": ").append(project.isArchived()).append(",\n");
        json.append("    \"createdAt\": \"").append(project.getCreatedAt()).append("\"\n");
        json.append("  },\n");
        json.append("  \"members\": [\n");
        for (int i = 0; i < members.size(); i++) {
            ProjectMember m = members.get(i);
            json.append("    { \"email\": \"").append(m.getUser().getEmail())
                    .append("\", \"name\": \"").append(m.getUser().getNom())
                    .append("\", \"role\": \"").append(m.getRole())
                    .append("\", \"joinedAt\": \"").append(m.getJoinedAt()).append("\" }");
            if (i < members.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]\n}");
        return json.toString().getBytes();
    }
}
