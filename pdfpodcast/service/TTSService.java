package com.pdfpodcast.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TTSService {

    @Value("${tts.elevenlabs.api-key}")
    private String apiKey;

    @Value("${tts.elevenlabs.api-url:https://api.elevenlabs.io/v1/text-to-speech}")
    private String apiUrl;

    @Value("${tts.max-text-length:500}")
    private int maxTextLength;

    @Value("${tts.chunk-target-length:320}")
    private int chunkTargetLength;

    @Value("${tts.enabled:false}")
    private boolean enabled;

    @Value("${tts.voice.host:}")
    private String hostVoiceId;

    @Value("${tts.voice.guest:}")
    private String guestVoiceId;

    @Value("${tts.elevenlabs.model-id:eleven_multilingual_v2}")
    private String modelId;

    @Value("${tts.elevenlabs.output-format:mp3_44100_128}")
    private String outputFormat;

    @Value("${tts.elevenlabs.optimize-streaming-latency:0}")
    private int optimizeStreamingLatency;

    @Value("${tts.voice.stability:0.45}")
    private double stability;

    @Value("${tts.voice.similarity-boost:0.8}")
    private double similarityBoost;

    @Value("${tts.voice.style:0.2}")
    private double style;

    @Value("${tts.voice.speaker-boost:true}")
    private boolean speakerBoost;

    @Value("${tts.retry.max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${tts.retry.delay-ms:1500}")
    private long retryDelayMs;

    private final RestTemplate restTemplate;

    public TTSService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "ttsCache", key = "#text.hashCode() + '_' + #voiceId")
    public byte[] generateVoice(String text, String voiceId) {
        try {
            if (!isReady()) {
                log.info("TTS not ready, skipping audio generation");
                return new byte[0];
            }

            if (text == null || text.trim().isEmpty()) {
                log.warn("Empty text provided for TTS generation");
                return new byte[0];
            }

            if (voiceId == null || voiceId.isBlank()) {
                log.warn("Voice ID missing, skipping audio generation");
                return new byte[0];
            }

            String url = UriComponentsBuilder.fromHttpUrl(apiUrl + "/" + voiceId)
                    .queryParam("output_format", outputFormat)
                    .queryParam("optimize_streaming_latency", optimizeStreamingLatency)
                    .toUriString();
            log.debug("Generating voice for voice: {}", voiceId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("xi-api-key", apiKey);
            headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("text", text);
            body.put("model_id", modelId);

            Map<String, Object> settings = new HashMap<>();
            settings.put("stability", stability);
            settings.put("similarity_boost", similarityBoost);
            settings.put("style", style);
            settings.put("speaker_boost", speakerBoost);
            body.put("voice_settings", settings);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            Exception lastError = null;
            for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
                try {
                    ResponseEntity<byte[]> response = restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            request,
                            byte[].class
                    );

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        log.info("Successfully generated voice for voiceId: {}", voiceId);
                        return response.getBody();
                    }
                    throw new IllegalStateException("TTS API returned non-success status: " + response.getStatusCode());
                } catch (Exception e) {
                    lastError = e;
                    if (attempt < retryMaxAttempts) {
                        Thread.sleep(retryDelayMs * attempt);
                    }
                }
            }
            throw lastError;

        } catch (RestClientResponseException e) {
            log.error("TTS API Error - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("TTS API Error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error generating voice: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate voice: " + e.getMessage(), e);
        }
    }

    public byte[] generateConversationAudio(String transcript, String hostName, String guestName) {
        if (!isReady() || transcript == null || transcript.isBlank()) {
            return new byte[0];
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (String line : transcript.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String voiceId = trimmed.startsWith(hostName + ":") ? hostVoiceId : guestVoiceId;
            String speechText = stripSpeakerLabel(trimmed);
            for (String chunk : chunkText(speechText)) {
                byte[] audioChunk = generateVoice(chunk, voiceId);
                if (audioChunk.length == 0) {
                    return new byte[0];
                }
                outputStream.write(audioChunk, 0, audioChunk.length);
            }
        }
        return outputStream.toByteArray();
    }

    public boolean isReady() {
        return enabled
                && apiKey != null && !apiKey.isBlank() && !"YOUR_NEW_API_KEY".equals(apiKey)
                && hostVoiceId != null && !hostVoiceId.isBlank()
                && guestVoiceId != null && !guestVoiceId.isBlank();
    }

    public Map<String, Object> getConfigurationStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", enabled);
        status.put("ready", isReady());
        status.put("apiConfigured", apiKey != null && !apiKey.isBlank() && !"YOUR_NEW_API_KEY".equals(apiKey));
        status.put("hostVoiceConfigured", hostVoiceId != null && !hostVoiceId.isBlank());
        status.put("guestVoiceConfigured", guestVoiceId != null && !guestVoiceId.isBlank());
        status.put("modelId", modelId);
        status.put("outputFormat", outputFormat);
        status.put("chunkTargetLength", chunkTargetLength);
        status.put("maxTextLength", maxTextLength);
        return status;
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        String[] sentences = normalized.split("(?<=[.!?])\\s+");
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.length() > maxTextLength) {
                flushChunk(chunks, current);
                chunks.addAll(splitHard(trimmed));
                continue;
            }

            int projectedLength = current.length() == 0 ? trimmed.length() : current.length() + 1 + trimmed.length();
            if (projectedLength > chunkTargetLength && current.length() > 0) {
                flushChunk(chunks, current);
            }

            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(trimmed);
        }

        flushChunk(chunks, current);
        return chunks;
    }

    private List<String> splitHard(String text) {
        List<String> parts = new ArrayList<>();
        for (int index = 0; index < text.length(); index += maxTextLength) {
            int end = Math.min(text.length(), index + maxTextLength);
            parts.add(text.substring(index, end));
        }
        return parts;
    }

    private void flushChunk(List<String> chunks, StringBuilder current) {
        if (current.length() > 0) {
            chunks.add(current.toString());
            current.setLength(0);
        }
    }

    private String stripSpeakerLabel(String line) {
        int separator = line.indexOf(':');
        if (separator < 0 || separator == line.length() - 1) {
            return line;
        }
        return line.substring(separator + 1).trim();
    }
}
