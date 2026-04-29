package com.pdfpodcast.controller;

import com.pdfpodcast.model.CreatePodcastRequest;
import com.pdfpodcast.model.PodcastJob;
import com.pdfpodcast.service.OpenAiScriptingService;
import com.pdfpodcast.service.FileStorageService;
import com.pdfpodcast.service.PodcastOrchestratorService;
import com.pdfpodcast.service.TTSService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api")
public class PodcastController {

    private final PodcastOrchestratorService podcastOrchestratorService;
    private final TTSService ttsService;
    private final OpenAiScriptingService openAiScriptingService;
    private final FileStorageService fileStorageService;

    public PodcastController(
            PodcastOrchestratorService podcastOrchestratorService,
            TTSService ttsService,
            OpenAiScriptingService openAiScriptingService,
            FileStorageService fileStorageService
    ) {
        this.podcastOrchestratorService = podcastOrchestratorService;
        this.ttsService = ttsService;
        this.openAiScriptingService = openAiScriptingService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/")
    public ResponseEntity<?> home() {
        return ResponseEntity.ok().body(java.util.Map.of(
                "name", "pdfpodcast",
                "status", "running",
                "ui", "/"
        ));
    }

    @PostMapping(value = "/podcasts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PodcastJob> createPodcast(@Valid @ModelAttribute CreatePodcastRequest request, Principal principal) {
        PodcastJob job = podcastOrchestratorService.createJob(request.getFile(), request, principal.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    @GetMapping("/podcasts/{jobId}")
    public ResponseEntity<PodcastJob> getPodcast(@PathVariable String jobId, Principal principal) {
        return ResponseEntity.ok(podcastOrchestratorService.getJob(jobId, principal.getName()));
    }

    @GetMapping("/config/tts")
    public ResponseEntity<?> getTtsConfiguration() {
        return ResponseEntity.ok(ttsService.getConfigurationStatus());
    }

    @GetMapping("/config/scripting")
    public ResponseEntity<?> getScriptingConfiguration() {
        return ResponseEntity.ok(openAiScriptingService.getConfigurationStatus());
    }

    @GetMapping(value = "/podcasts/{jobId}/transcript", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getTranscript(@PathVariable String jobId, Principal principal) {
        PodcastJob job = podcastOrchestratorService.getCompletedJob(jobId, principal.getName());
        return ResponseEntity.ok(job.getTranscript());
    }

    @GetMapping("/podcasts/{jobId}/audio")
    public ResponseEntity<byte[]> getAudio(@PathVariable String jobId, Principal principal) throws Exception {
        PodcastJob job = podcastOrchestratorService.getCompletedJob(jobId, principal.getName());
        if (!job.isAudioAvailable() || job.getAudioPath() == null || job.getAudioPath().isBlank()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .body(new byte[0]);
        }

        byte[] audioBytes = fileStorageService.readAudio(job.getAudioPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getId() + ".mp3\"")
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audioBytes);
    }
}
