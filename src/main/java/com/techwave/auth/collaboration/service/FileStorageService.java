package com.techwave.auth.collaboration.service;

import com.techwave.auth.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Service de stockage de fichiers sur le système de fichiers local.
 * En production, peut être remplacé par S3, MinIO, etc.
 */
@Service
public class FileStorageService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * Taille maximale en octets : 100 Mo.
     */
    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;

    /**
     * Types MIME acceptés.
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml", "image/bmp",
            "application/pdf",
            "application/zip", "application/x-zip-compressed",
            "text/csv",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/vnd.openxmlformats-officedocument.presentationml.presentation" // .pptx
    );

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(uploadDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le répertoire d'upload : " + uploadDir, e);
        }
    }

    /**
     * Enregistre un fichier uploadé et retourne le nom unique stocké.
     *
     * @return le nom du fichier stocké (UUID + extension)
     */
    public String storeFile(MultipartFile file) {
        validateFile(file);

        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String storedFileName = UUID.randomUUID().toString() + extension;
        Path targetPath = Paths.get(uploadDir).resolve(storedFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du fichier", e);
        }

        return storedFileName;
    }

    /**
     * Retourne le chemin absolu d'un fichier stocké.
     */
    public Path getFilePath(String storedFileName) {
        return Paths.get(uploadDir).resolve(storedFileName).normalize();
    }

    /**
     * Supprime un fichier du stockage.
     */
    public void deleteFile(String storedFileName) {
        try {
            Path path = getFilePath(storedFileName);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Log mais ne pas bloquer la suppression de l'entité
            System.err.println("Avertissement : impossible de supprimer le fichier " + storedFileName);
        }
    }

    /**
     * Valide le fichier uploadé (taille, type).
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Le fichier est vide");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("Le fichier dépasse la taille maximale de 100 Mo");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isContentTypeAllowed(contentType)) {
            throw new BusinessException(
                    "Type de fichier non autorisé : " + contentType +
                            ". Types acceptés : images, PDF, ZIP, CSV, Office"
            );
        }
    }

    private boolean isContentTypeAllowed(String contentType) {
        // Accepte tous les types image/*
        if (contentType.startsWith("image/")) return true;
        return ALLOWED_CONTENT_TYPES.contains(contentType);
    }
}
