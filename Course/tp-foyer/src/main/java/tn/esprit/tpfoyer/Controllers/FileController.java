package tn.esprit.tpfoyer.Controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/courses/files")
public class FileController {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = Paths.get(uploadDir).resolve(filename).toAbsolutePath().normalize();
            Resource resource = new UrlResource(file.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            String contentType = determineContentType(filename);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/**")
    public ResponseEntity<Resource> serveFileNested(HttpServletRequest request) {
        try {
            String requestUri = request.getRequestURI();
            String prefix = "/api/courses/files/";
            int idx = requestUri.indexOf(prefix);
            if (idx < 0) {
                return ResponseEntity.notFound().build();
            }
            String relativePath = requestUri.substring(idx + prefix.length());
            if (relativePath.isBlank()) {
                return ResponseEntity.notFound().build();
            }

            Path file = Paths.get(uploadDir).resolve(relativePath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(file.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String filename = Paths.get(relativePath).getFileName().toString();
            String contentType = determineContentType(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{subfolder}/{filename:.+}")
    public ResponseEntity<Resource> serveFileInSubfolder(
            @PathVariable String subfolder,
            @PathVariable String filename) {
        try {
            Path file = Paths.get(uploadDir)
                    .resolve(subfolder)
                    .resolve(filename)
                    .toAbsolutePath()
                    .normalize();
            Resource resource = new UrlResource(file.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            String contentType = determineContentType(filename);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }
}
