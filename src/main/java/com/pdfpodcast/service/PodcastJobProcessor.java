package com.pdfpodcast.service;

import com.pdfpodcast.model.ContentAnalysis;
import com.pdfpodcast.model.ConversationPlan;
import com.pdfpodcast.model.CreatePodcastRequest;
import com.pdfpodcast.model.PodcastJob;
import com.pdfpodcast.model.PodcastJobStatus;
import com.pdfpodcast.repository.PodcastJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PodcastJobProcessor {

    private final PodcastJobRepository podcastJobRepository;
    private final PdfService pdfService;
    private final ContentAnalysisService contentAnalysisService;
    private final ConversationPlannerService conversationPlannerService;
    private final OpenAiScriptingService openAiScriptingService;
    private final TTSService ttsService;
    private final FileStorageService fileStorageService;
    private final TaskExecutor taskExecutor;

    public PodcastJobProcessor(
            PodcastJobRepository podcastJobRepository,
            PdfService pdfService,
            ContentAnalysisService contentAnalysisService,
            ConversationPlannerService conversationPlannerService,
            OpenAiScriptingService openAiScriptingService,
            TTSService ttsService,
            FileStorageService fileStorageService,
            @Qualifier("podcastTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.podcastJobRepository = podcastJobRepository;
        this.pdfService = pdfService;
        this.contentAnalysisService = contentAnalysisService;
        this.conversationPlannerService = conversationPlannerService;
        this.openAiScriptingService = openAiScriptingService;
        this.ttsService = ttsService;
        this.fileStorageService = fileStorageService;
        this.taskExecutor = taskExecutor;
    }

    @Scheduled(fixedDelayString = "${queue.poll-delay-ms:3000}")
    public void pollAndProcessQueuedJobs() {
        List<PodcastJob> jobs = podcastJobRepository.findTop5ByStatusInAndNextAttemptAtBeforeOrderByCreatedAtAsc(
                List.of(PodcastJobStatus.QUEUED, PodcastJobStatus.RETRY_PENDING),
                LocalDateTime.now().plusSeconds(1)
        );
        for (PodcastJob job : jobs) {
            taskExecutor.execute(() -> process(job.getId()));
        }
    }

    public void process(String jobId) {
        PodcastJob job = podcastJobRepository.findById(jobId).orElse(null);
        if (job == null || (job.getStatus() != PodcastJobStatus.QUEUED && job.getStatus() != PodcastJobStatus.RETRY_PENDING)) {
            return;
        }

        try {
            job.setAttempts(job.getAttempts() + 1);
            job.setStatus(PodcastJobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            update(job, 10, "Extracting PDF text");

            String extractedText = pdfService.extractText(job.getSourceFilePath());

            update(job, 25, "Chunking document for AI analysis");
            List<String> chunks = openAiScriptingService.chunkDocument(extractedText);

            update(job, 40, "Analyzing content");
            CreatePodcastRequest request = toRequest(job);
            ContentAnalysis analysis = openAiScriptingService.analyzeContent(chunks, job.getTitle(), request);
            if (analysis == null || analysis.getKeyIdeas().isEmpty()) {
                analysis = contentAnalysisService.analyze(extractedText, job.getTitle());
            }
            job.setTitle(analysis.getTitle());
            job.setSummary(analysis.getShortSummary());
            job.setSections(analysis.getSections());
            podcastJobRepository.save(job);

            update(job, 58, "Planning conversation");
            ConversationPlan plan = openAiScriptingService.buildConversationPlan(chunks, analysis, request);
            if (plan == null || plan.getBeats().isEmpty()) {
                plan = conversationPlannerService.buildPlan(analysis, request);
            }

            update(job, 78, "Generating script with OpenAI");
            String transcript = openAiScriptingService.generateTranscript(chunks, analysis, plan, request);
            validateTranscript(transcript, request);
            job.setTranscript(transcript);
            job.setTranscriptAvailable(true);
            podcastJobRepository.save(job);

            update(job, 90, "Synthesizing audio");
            try {
                byte[] audioBytes = ttsService.generateConversationAudio(transcript, request.getHostName(), request.getGuestName());
                if (audioBytes.length > 0) {
                    job.setAudioPath(fileStorageService.storeAudio(job.getId(), audioBytes));
                    job.setAudioAvailable(true);
                    finish(job, PodcastJobStatus.COMPLETED, "Podcast ready");
                } else {
                    job.setAudioAvailable(false);
                    finish(job, PodcastJobStatus.COMPLETED_WITHOUT_AUDIO, "Transcript ready, audio unavailable");
                }
            } catch (Exception ttsError) {
                log.error("Audio generation failed for job {}", jobId, ttsError);
                job.setAudioAvailable(false);
                job.setErrorMessage("Transcript generated, audio unavailable: " + ttsError.getMessage());
                finish(job, PodcastJobStatus.COMPLETED_WITHOUT_AUDIO, "Transcript ready, audio generation failed");
            }
        } catch (Exception e) {
            log.error("Podcast job {} failed", jobId, e);
            if (job.getAttempts() < 3) {
                job.setStatus(PodcastJobStatus.RETRY_PENDING);
                job.setNextAttemptAt(LocalDateTime.now().plusMinutes(job.getAttempts()));
                job.setErrorMessage(e.getMessage());
                update(job, job.getProgress(), "Retry scheduled");
            } else {
                job.setErrorMessage(e.getMessage());
                finish(job, PodcastJobStatus.FAILED, "Generation failed");
            }
        }
    }

    private void update(PodcastJob job, int progress, String message) {
        job.setProgress(progress);
        job.setProgressMessage(message);
        job.setUpdatedAt(LocalDateTime.now());
        podcastJobRepository.save(job);
    }

    private void finish(PodcastJob job, PodcastJobStatus status, String message) {
        job.setStatus(status);
        job.setProgress(100);
        job.setProgressMessage(message);
        job.setCompletedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        podcastJobRepository.save(job);
    }

    private CreatePodcastRequest toRequest(PodcastJob job) {
        CreatePodcastRequest request = new CreatePodcastRequest();
        request.setTitle(job.getTitle());
        request.setAudience(job.getAudience());
        request.setStyle(job.getStyle());
        request.setDifficulty(job.getDifficulty());
        request.setTargetDurationMinutes(job.getTargetDurationMinutes());
        request.setHostName(job.getHostName());
        request.setGuestName(job.getGuestName());
        request.setHostRole(job.getHostRole());
        request.setGuestRole(job.getGuestRole());
        request.setIncludeQna(job.isIncludeQna());
        request.setIncludeTakeaways(job.isIncludeTakeaways());
        return request;
    }

    private void validateTranscript(String transcript, CreatePodcastRequest request) {
        if (transcript == null || transcript.isBlank()) {
            throw new IllegalStateException("Generated transcript is empty");
        }
        long invalidLines = transcript.lines()
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith(request.getHostName() + ":") && !line.startsWith(request.getGuestName() + ":"))
                .count();
        if (invalidLines > 0) {
            throw new IllegalStateException("Generated transcript has invalid speaker formatting");
        }
    }
}
