package com.hantu.file_service.service;

import com.hantu.file_service.dto.response.FileDeleteResponse;
import com.hantu.file_service.dto.response.FileUploadResponse;
import com.hantu.file_service.exception.AppException;
import com.hantu.file_service.exception.ErrorCode;
import com.hantu.file_service.model.FileCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/heic",
            "image/heif"
    );
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "webp",
            "gif",
            "heic",
            "heif"
    );

    @Value("${app.file.storage-root:storage}")
    private String storageRoot;

    public FileUploadResponse upload(String categoryRaw, MultipartFile file) {
        FileCategory category = FileCategory.from(categoryRaw);
        String userId = currentUserId();

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.EMPTY_FILE);
        }
        validateImageType(file);

        String generatedFileName = buildSafeFileName(file.getOriginalFilename());
        Path userFolder = storageRootPath().resolve(category.folder()).resolve(userId).normalize();
        ensureInsideStorageRoot(userFolder);

        long usedBytesBefore = calculateDirectorySize(userFolder);
        if (usedBytesBefore + file.getSize() > category.maxBytes()) {
            throw new AppException(ErrorCode.STORAGE_QUOTA_EXCEEDED);
        }

        Path targetPath = userFolder.resolve(generatedFileName).normalize();
        ensureInsideStorageRoot(targetPath);

        try {
            Files.createDirectories(userFolder);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AppException(ErrorCode.STORAGE_IO_ERROR);
        }

        long usedBytesAfter = calculateDirectorySize(userFolder);
        String relativePath = category.folder() + "/" + userId + "/" + generatedFileName;

        return FileUploadResponse.builder()
                .category(category.folder())
                .userId(userId)
                .fileName(generatedFileName)
                .relativePath(relativePath)
                .contentType(file.getContentType())
                .size(file.getSize())
                .usedBytes(usedBytesAfter)
                .maxBytes(category.maxBytes())
                .build();
    }

    public FileDeleteResponse delete(String categoryRaw, String fileName) {
        FileCategory category = FileCategory.from(categoryRaw);
        String userId = currentUserId();
        String safeFileName = sanitizeFileName(fileName);
        Path userFolder = storageRootPath().resolve(category.folder()).resolve(userId).normalize();
        Path targetPath = userFolder.resolve(safeFileName).normalize();

        ensureInsideStorageRoot(targetPath);

        if (!Files.exists(targetPath)) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND);
        }

        try {
            Files.delete(targetPath);
        } catch (IOException e) {
            throw new AppException(ErrorCode.STORAGE_IO_ERROR);
        }

        long usedBytesAfter = calculateDirectorySize(userFolder);
        String relativePath = category.folder() + "/" + userId + "/" + safeFileName;

        return FileDeleteResponse.builder()
                .category(category.folder())
                .userId(userId)
                .fileName(safeFileName)
                .relativePath(relativePath)
                .usedBytes(usedBytesAfter)
                .maxBytes(category.maxBytes())
                .build();
    }

    public FileReadResult readPublic(String relativePath) {
        String sanitizedRelativePath = sanitizeRelativePath(relativePath);
        Path targetPath = storageRootPath().resolve(sanitizedRelativePath).normalize();
        ensureInsideStorageRoot(targetPath);

        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND);
        }

        String contentType = detectContentType(targetPath);
        if (!ALLOWED_IMAGE_MIME_TYPES.contains(contentType)) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }

        try {
            byte[] bytes = Files.readAllBytes(targetPath);
            Resource resource = new ByteArrayResource(bytes);
            return new FileReadResult(resource, MediaType.parseMediaType(contentType));
        } catch (IOException e) {
            throw new AppException(ErrorCode.STORAGE_IO_ERROR);
        }
    }

    private String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getName();
    }

    private Path storageRootPath() {
        return Path.of(storageRoot).toAbsolutePath().normalize();
    }

    private String buildSafeFileName(String originalFileName) {
        String sanitized = sanitizeFileName(Objects.toString(originalFileName, ""));
        String extension = "";
        int dotIndex = sanitized.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < sanitized.length() - 1) {
            extension = sanitized.substring(dotIndex);
            sanitized = sanitized.substring(0, dotIndex);
        }
        String uniquePart = Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
        return sanitized + "-" + uniquePart + extension;
    }

    private String sanitizeFileName(String fileName) {
        String normalized = Normalizer.normalize(fileName, Normalizer.Form.NFKC).trim();
        String baseName = Path.of(normalized).getFileName().toString();
        String safe = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe.isBlank() || ".".equals(safe) || "..".equals(safe)) {
            throw new AppException(ErrorCode.INVALID_FILE_NAME);
        }
        return safe;
    }

    private void ensureInsideStorageRoot(Path path) {
        Path root = storageRootPath();
        if (!path.startsWith(root)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateImageType(MultipartFile file) {
        String contentType = Objects.toString(file.getContentType(), "").toLowerCase(Locale.ROOT);
        String originalName = sanitizeFileName(Objects.toString(file.getOriginalFilename(), ""));
        String extension = extractExtension(originalName);

        // Double-check both MIME type and extension to reduce spoofed uploads.
        if (!ALLOWED_IMAGE_MIME_TYPES.contains(contentType) || !ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeRelativePath(String relativePath) {
        String normalized = Objects.toString(relativePath, "").trim().replace('\\', '/');
        String[] parts = normalized.split("/");
        if (parts.length != 3) {
            throw new AppException(ErrorCode.INVALID_FILE_NAME);
        }

        FileCategory.from(parts[0]);
        String userId = parts[1];
        if (userId.isBlank() || userId.contains("..")) {
            throw new AppException(ErrorCode.INVALID_FILE_NAME);
        }

        String fileName = sanitizeFileName(parts[2]);
        return parts[0] + "/" + userId + "/" + fileName;
    }

    private String detectContentType(Path targetPath) {
        try {
            String contentType = Files.probeContentType(targetPath);
            if (contentType == null || contentType.isBlank()) {
                String extension = extractExtension(targetPath.getFileName().toString());
                return switch (extension) {
                    case "jpg", "jpeg" -> "image/jpeg";
                    case "png" -> "image/png";
                    case "webp" -> "image/webp";
                    case "gif" -> "image/gif";
                    case "heic" -> "image/heic";
                    case "heif" -> "image/heif";
                    default -> throw new AppException(ErrorCode.INVALID_FILE_TYPE);
                };
            }
            return contentType.toLowerCase(Locale.ROOT);
        } catch (IOException e) {
            throw new AppException(ErrorCode.STORAGE_IO_ERROR);
        }
    }

    private long calculateDirectorySize(Path path) {
        if (!Files.exists(path)) {
            return 0L;
        }
        try (Stream<Path> files = Files.walk(path)) {
            return files
                    .filter(Files::isRegularFile)
                    .mapToLong(this::fileSizeOrZero)
                    .sum();
        } catch (IOException e) {
            throw new AppException(ErrorCode.STORAGE_IO_ERROR);
        }
    }

    private long fileSizeOrZero(Path filePath) {
        try {
            return Files.size(filePath);
        } catch (IOException e) {
            return 0L;
        }
    }

    public record FileReadResult(Resource resource, MediaType contentType) {
    }
}
