package com.esprit.demo.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;    // 5 MB
    private static final long MAX_FILE_SIZE_BYTES  = 100L * 1024 * 1024;  // 100 MB

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "video/mp4", "video/webm", "video/ogg", "video/quicktime",
            "application/pdf");

    /** Store an image file (JPEG/PNG/GIF/WEBP, max 5 MB). */
    public String store(MultipartFile file, String sub) {
        if (file == null || file.isEmpty()) return null;

        String contentType = file.getContentType();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType))
            throw new IllegalArgumentException("Type de fichier non supporté. Utilisez JPEG, PNG, GIF ou WEBP.");

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES)
            throw new IllegalArgumentException("Le fichier dépasse la taille maximale de 5 MB.");

        return save(file, sub);
    }

    /** Store a video or PDF file (MP4/WebM/OGG/MOV/PDF, max 100 MB). Returns URL + MIME type. */
    public StoredFile storeFile(MultipartFile file, String sub) {
        if (file == null || file.isEmpty()) return null;

        String contentType = file.getContentType();
        if (!ALLOWED_FILE_TYPES.contains(contentType))
            throw new IllegalArgumentException(
                    "Type non supporté. Utilisez MP4, WebM, OGG, MOV ou PDF.");

        if (file.getSize() > MAX_FILE_SIZE_BYTES)
            throw new IllegalArgumentException("Le fichier dépasse la taille maximale de 100 MB.");

        String url = save(file, sub);
        return new StoredFile(url, contentType);
    }

    /** Shared save logic — writes file to disk and returns URL. */
    private String save(MultipartFile file, String sub) {
        try {
            Path dir = Paths.get(uploadDir, sub);
            Files.createDirectories(dir);
            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + ext;
            Path dest = dir.resolve(filename);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + sub + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde du fichier : " + e.getMessage());
        }
    }

    /** Delete a file by its relative URL. */
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        try {
            String relativePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Files.deleteIfExists(Paths.get(relativePath));
        } catch (IOException e) {
            System.err.println("Impossible de supprimer le fichier : " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    /** Value object returned by storeFile(). */
    public record StoredFile(String url, String mimeType) {}
}
