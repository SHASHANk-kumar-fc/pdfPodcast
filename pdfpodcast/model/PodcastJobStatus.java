package com.pdfpodcast.model;

public enum PodcastJobStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    COMPLETED_WITHOUT_AUDIO,
    RETRY_PENDING,
    FAILED
}
