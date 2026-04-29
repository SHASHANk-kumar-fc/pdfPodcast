package com.pdfpodcast.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PodcastService {

    public String convertToPodcast(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                log.warn("Empty text provided for podcast conversion");
                return "";
            }

            log.info("Converting text to podcast format, text length: {}", text.length());

            // Split by sentences more intelligently
            String[] sentences = text.split("(?<=[.!?])\\s+");

            StringBuilder podcast = new StringBuilder();
            boolean isHost1 = true;
            int sentenceCount = 0;

            for (String sentence : sentences) {
                String trimmed = sentence.trim();

                // Skip empty sentences or very short fragments
                if (trimmed.isEmpty() || trimmed.length() < 3) {
                    continue;
                }

                // Ensure sentence ends with punctuation
                if (!trimmed.matches(".*[.!?]$")) {
                    trimmed = trimmed + ".";
                }

                String hostLabel = isHost1 ? "Host 1" : "Host 2";
                podcast.append(hostLabel).append(": ").append(trimmed).append("\n");

                isHost1 = !isHost1;
                sentenceCount++;
            }

            log.info("Successfully converted {} sentences to podcast format", sentenceCount);
            return podcast.toString();

        } catch (Exception e) {
            log.error("Error converting text to podcast: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert text to podcast: " + e.getMessage(), e);
        }
    }

    /**
     * Alternate method for more context-aware podcast generation
     */
    public String convertToPodcastWithPauses(String text, int pauseMillis) {
        String podcastText = convertToPodcast(text);
        // Could add metadata for pause durations between sentences
        return podcastText;
    }
}
