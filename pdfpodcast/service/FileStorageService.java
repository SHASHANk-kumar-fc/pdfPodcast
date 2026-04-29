package com.pdfpodcast.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class FileStorageService {

    private final Path storageRoot;

    public FileStorageService(@Value("${storage.local.root:storage}") String storageRoot) {
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    public String storeAudio(String jobId, byte[] bytes) throws IOException {
        Files.createDirectories(storageRoot);
        Path outputPath = storageRoot.resolve(jobId + ".mp3");
        Files.write(outputPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return outputPath.toString();
    }

    public String storeUpload(String jobId, MultipartFile file) throws IOException {
        Files.createDirectories(storageRoot);
        String filename = file.getOriginalFilename() == null ? "upload.pdf" : file.getOriginalFilename().
                                                                              replaceAll("[^a-zA-Z0-9._-]", "_");
        Path outputPath = storageRoot.resolve(jobId + "-" + filename);
        Files.write(outputPath, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return outputPath.toString();
    }

    public byte[] readAudio(String audioPath) throws IOException {
        return Files.readAllBytes(Paths.get(audioPath));
    }
}
