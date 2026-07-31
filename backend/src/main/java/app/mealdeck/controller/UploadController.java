package app.mealdeck.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import app.mealdeck.dto.UploadResponse;

@RestController
@RequestMapping("/api/uploads")
/**
 * Validates and stores meal photos on the backend filesystem.
 */
public class UploadController {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");
    private final Path uploadDirectory;

    /**
     * Creates an upload controller rooted at the configured directory.
     *
     * @param uploadDirectory filesystem directory for uploaded images
     */
    public UploadController(@Value("${mealdeck.upload-dir:./data/uploads}") String uploadDirectory) {
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    @PostMapping
    /**
     * Stores a supported image and returns its public application path.
     *
     * @param file image submitted as multipart form data
     * @return public URL path for the stored image
     */
    public UploadResponse upload(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a photo first");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(contentType)) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only meal photos are supported");
        if (file.getSize() > 10_000_000) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Photo must be under 10 MB");
        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/heic" -> ".heic";
            case "image/heif" -> ".heif";
            default -> ".jpg";
        };
        try {
            Files.createDirectories(uploadDirectory);
            String name = UUID.randomUUID() + ext;
            Path target = uploadDirectory.resolve(name).normalize();
            if (!target.startsWith(uploadDirectory)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new UploadResponse("/uploads/" + name);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store photo", ex);
        }
    }
}
