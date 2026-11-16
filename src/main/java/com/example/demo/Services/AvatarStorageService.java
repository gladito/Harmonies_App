package com.example.demo.Services;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AvatarStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif", "svg");
    private static final Map<String, MediaType> CONTENT_TYPES = Map.of(
            "png", MediaType.IMAGE_PNG,
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG,
            "webp", MediaType.parseMediaType("image/webp"),
            "gif", MediaType.IMAGE_GIF,
            "svg", MediaType.parseMediaType("image/svg+xml")
    );

    private final Path storageDir = Paths.get("data", "avatars").toAbsolutePath().normalize();

    public AvatarStorageService() {
        try {
            Files.createDirectories(storageDir);
        } catch (IOException ignored) {
        }
    }

    public String storeAvatar(String prefix, Long entityId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Seleccioná un archivo de imagen válido.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar los 5 MB.");
        }

        String ext = extractExtension(file.getOriginalFilename(), file.getContentType());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Formato no soportado. Usá PNG, JPG, WEBP, GIF o SVG.");
        }

        String safePrefix = (prefix == null ? "avatar" : prefix.replaceAll("[^a-zA-Z0-9_-]", ""));
        String filename = safePrefix + "-" + (entityId != null ? entityId : 0) + "-"
                + UUID.randomUUID().toString().substring(0, 8) + "." + ext;

        try {
            Files.createDirectories(storageDir);
            Path target = storageDir.resolve(filename).normalize();
            if (!target.startsWith(storageDir)) {
                throw new IllegalArgumentException("Nombre de archivo inválido.");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/avatars/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen de perfil.");
        }
    }

    public String normalizeAvatarUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("La URL de la foto no puede superar los 255 caracteres.");
        }
        if (trimmed.startsWith("/images/") || trimmed.startsWith("/avatars/")) {
            if (trimmed.contains("..") || trimmed.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("Ruta de imagen inválida.");
            }
            return trimmed;
        }
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null || (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
            return trimmed;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Ingresá una URL de imagen válida (https://...) o subí un archivo.");
        }
    }

    public Path resolveStoredAvatar(String filename) {
        if (filename == null || filename.isBlank() || !filename.matches("[a-zA-Z0-9._-]+") || filename.contains("..")) {
            return null;
        }
        Path candidate = storageDir.resolve(filename).normalize();
        if (!candidate.startsWith(storageDir) || !Files.isRegularFile(candidate)) {
            return null;
        }
        return candidate;
    }

    public MediaType mediaTypeFor(String filename) {
        if (filename == null) return MediaType.APPLICATION_OCTET_STREAM;
        int dot = filename.lastIndexOf('.');
        String ext = dot >= 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        return CONTENT_TYPES.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM);
    }

    private String extractExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT).trim();
            if (ALLOWED_EXTENSIONS.contains(ext)) {
                return ext;
            }
        }
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.contains("png")) return "png";
            if (ct.contains("jpeg") || ct.contains("jpg")) return "jpg";
            if (ct.contains("webp")) return "webp";
            if (ct.contains("gif")) return "gif";
            if (ct.contains("svg")) return "svg";
        }
        return "";
    }
}
