package com.yatayat.backend.service;

import com.yatayat.backend.entity.DriverDocumentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class DriverDocumentStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "application/pdf"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final Path rootDirectory;

    public DriverDocumentStorageService(
            @Value("${driver.upload.directory:uploads/drivers}")
            String uploadDirectory
    ) {
        this.rootDirectory = Path.of(uploadDirectory)
                .toAbsolutePath()
                .normalize();
    }

    public StoredDriverFile store(
            MultipartFile file,
            Long driverProfileId,
            DriverDocumentType documentType
    ) {
        validateFile(file);

        try {
            String originalFileName = sanitizeFileName(
                    file.getOriginalFilename()
            );

            String extension = getExtension(originalFileName);

            String storedFileName =
                    documentType.name().toLowerCase()
                            + "-"
                            + UUID.randomUUID()
                            + extension;

            Path driverDirectory = rootDirectory
                    .resolve(String.valueOf(driverProfileId))
                    .resolve(documentType.name().toLowerCase())
                    .normalize();

            Files.createDirectories(driverDirectory);

            Path destination = driverDirectory
                    .resolve(storedFileName)
                    .normalize();

            if (!destination.startsWith(rootDirectory)) {
                throw new IllegalArgumentException(
                        "Invalid upload destination"
                );
            }

            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return new StoredDriverFile(
                    originalFileName,
                    storedFileName,
                    destination.toString(),
                    file.getContentType(),
                    file.getSize()
            );

        } catch (IOException exception) {
            throw new RuntimeException(
                    "Unable to save driver document",
                    exception
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Required document is missing"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Each document must be 5 MB or smaller"
            );
        }

        String contentType = file.getContentType();

        if (
                contentType == null ||
                        !ALLOWED_CONTENT_TYPES.contains(contentType)
        ) {
            throw new IllegalArgumentException(
                    "Only JPG, PNG, and PDF files are allowed"
            );
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "document";
        }

        String cleanName = Path.of(fileName)
                .getFileName()
                .toString();

        return cleanName.replaceAll(
                "[^a-zA-Z0-9._-]",
                "_"
        );
    }

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");

        if (lastDot < 0) {
            return "";
        }

        return fileName.substring(lastDot).toLowerCase();
    }

    public record StoredDriverFile(
            String originalFileName,
            String storedFileName,
            String filePath,
            String contentType,
            long size
    ) {
    }
}