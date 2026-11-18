package com.talentflow.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResumeStorageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf");

    private final Path resumeDirectory;

    public ResumeStorageService(@Value("${app.upload-dir:./uploads}") String uploadDirectory) {
        this.resumeDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize().resolve("resumes");
    }

    public String validateAndGetContentType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please attach your resume");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume must be 10 MB or smaller");
        }

        String originalName = file.getOriginalFilename();
        String extension = originalName == null
                ? ""
                : originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please upload your CV/resume as a PDF. Word documents, certificates, and other file types are not supported.");
        }

        return MediaType.APPLICATION_PDF_VALUE;
    }

    public String store(MultipartFile file) throws IOException {
        validateAndGetContentType(file);
        Files.createDirectories(resumeDirectory);
        String originalName = file.getOriginalFilename();
        String extension = originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        String storedName = UUID.randomUUID() + extension;
        Path destination = resumeDirectory.resolve(storedName).normalize();
        if (!destination.startsWith(resumeDirectory)) {
            throw new IOException("Invalid resume storage path");
        }
        file.transferTo(destination);
        return destination.toString();
    }

    public Path resolve(String storedPath) throws IOException {
        Path path = Paths.get(storedPath).toAbsolutePath().normalize();
        if (!path.startsWith(resumeDirectory) || !Files.isRegularFile(path)) {
            throw new IOException("Resume file is unavailable");
        }
        return path;
    }

    public void delete(String storedPath) throws IOException {
        Path path = Paths.get(storedPath).toAbsolutePath().normalize();
        if (!path.startsWith(resumeDirectory)) {
            throw new IOException("Invalid resume storage path");
        }
        Files.deleteIfExists(path);
    }
}
