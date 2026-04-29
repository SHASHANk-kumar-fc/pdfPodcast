package com.pdfpodcast.service;

import com.pdfpodcast.model.CreatePodcastRequest;
import com.pdfpodcast.model.PodcastJob;
import com.pdfpodcast.model.PodcastJobStatus;
import com.pdfpodcast.repository.PodcastJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PodcastOrchestratorService {

    private final PodcastJobRepository podcastJobRepository;
    private final FileStorageService fileStorageService;

    public PodcastOrchestratorService(PodcastJobRepository podcastJobRepository, FileStorageService fileStorageService) {
        this.podcastJobRepository = podcastJobRepository;
        this.fileStorageService = fileStorageService;
    }

    public PodcastJob createJob(MultipartFile file, CreatePodcastRequest request, String ownerUsername) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A PDF file is required");
        }

        try {
            PodcastJob job = new PodcastJob();
            job.setId(UUID.randomUUID().toString());
            job.setStatus(PodcastJobStatus.QUEUED);
            job.setSourceFilename(file.getOriginalFilename());
            job.setSourceFilePath(fileStorageService.storeUpload(job.getId(), file));
            job.setTitle(firstNonBlank(request.getTitle(), file.getOriginalFilename(), "Untitled Podcast"));
            job.setAudience(request.getAudience());
            job.setStyle(request.getStyle());
            job.setDifficulty(request.getDifficulty());
            job.setHostName(request.getHostName());
            job.setGuestName(request.getGuestName());
            job.setHostRole(request.getHostRole());
            job.setGuestRole(request.getGuestRole());
            job.setIncludeQna(request.isIncludeQna());
            job.setIncludeTakeaways(request.isIncludeTakeaways());
            job.setTargetDurationMinutes(request.getTargetDurationMinutes());
            job.setOwnerUsername(ownerUsername);
            job.setProgress(0);
            job.setProgressMessage("Queued");
            job.setAttempts(0);
            job.setCreatedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            job.setNextAttemptAt(LocalDateTime.now());
            return podcastJobRepository.save(job);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create podcast job");
        }
    }

    public PodcastJob getJob(String jobId, String ownerUsername) {
        return podcastJobRepository.findByIdAndOwnerUsername(jobId, ownerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Podcast job not found"));
    }

    public PodcastJob getCompletedJob(String jobId, String ownerUsername) {
        PodcastJob job = getJob(jobId, ownerUsername);
        if (job.getStatus() == PodcastJobStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, job.getErrorMessage());
        }
        if (job.getStatus() == PodcastJobStatus.QUEUED
                || job.getStatus() == PodcastJobStatus.PROCESSING
                || job.getStatus() == PodcastJobStatus.RETRY_PENDING) {
            throw new ResponseStatusException(HttpStatus.ACCEPTED, "Podcast job is still processing");
        }
        return job;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
