package com.pdfpodcast.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "podcast_jobs")
public class PodcastJob {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PodcastJobStatus status;

    @Column(nullable = false)
    private String sourceFilename;

    @Column(nullable = false)
    private String sourceFilePath;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String audience;

    @Column(nullable = false)
    private String style;

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false)
    private String hostName;

    @Column(nullable = false)
    private String guestName;

    @Column(nullable = false)
    private String hostRole;

    @Column(nullable = false)
    private String guestRole;

    private boolean includeQna;
    private boolean includeTakeaways;

    private Integer targetDurationMinutes;

    private int progress;

    @Column(length = 255)
    private String progressMessage;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(length = 1000)
    private String summary;

    @Column(length = 1000)
    private String errorMessage;

    private boolean audioAvailable;
    private boolean transcriptAvailable;

    @Column(length = 512)
    private String audioPath;

    @Column(length = 64)
    private String ownerUsername;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime nextAttemptAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "podcast_job_sections", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "section_value", length = 500)
    private List<String> sections = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PodcastJobStatus getStatus() {
        return status;
    }

    public void setStatus(PodcastJobStatus status) {
        this.status = status;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public void setSourceFilename(String sourceFilename) {
        this.sourceFilename = sourceFilename;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public void setSourceFilePath(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getHostRole() {
        return hostRole;
    }

    public void setHostRole(String hostRole) {
        this.hostRole = hostRole;
    }

    public String getGuestRole() {
        return guestRole;
    }

    public void setGuestRole(String guestRole) {
        this.guestRole = guestRole;
    }

    public boolean isIncludeQna() {
        return includeQna;
    }

    public void setIncludeQna(boolean includeQna) {
        this.includeQna = includeQna;
    }

    public boolean isIncludeTakeaways() {
        return includeTakeaways;
    }

    public void setIncludeTakeaways(boolean includeTakeaways) {
        this.includeTakeaways = includeTakeaways;
    }

    public Integer getTargetDurationMinutes() {
        return targetDurationMinutes;
    }

    public void setTargetDurationMinutes(Integer targetDurationMinutes) {
        this.targetDurationMinutes = targetDurationMinutes;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getProgressMessage() {
        return progressMessage;
    }

    public void setProgressMessage(String progressMessage) {
        this.progressMessage = progressMessage;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isAudioAvailable() {
        return audioAvailable;
    }

    public void setAudioAvailable(boolean audioAvailable) {
        this.audioAvailable = audioAvailable;
    }

    public boolean isTranscriptAvailable() {
        return transcriptAvailable;
    }

    public void setTranscriptAvailable(boolean transcriptAvailable) {
        this.transcriptAvailable = transcriptAvailable;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(LocalDateTime nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public List<String> getSections() {
        return sections;
    }

    public void setSections(List<String> sections) {
        this.sections = sections;
    }
}
