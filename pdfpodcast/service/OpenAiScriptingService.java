package com.pdfpodcast.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdfpodcast.model.ContentAnalysis;
import com.pdfpodcast.model.ConversationPlan;
import com.pdfpodcast.model.CreatePodcastRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenAiScriptingService {

    @Value("${openai.enabled:true}")
    private boolean enabled;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.api-url:https://api.openai.com/v1/responses}")
    private String apiUrl;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    @Value("${openai.chunk-size:2400}")
    private int chunkSize;

    @Value("${openai.max-context-chunks:6}")
    private int maxContextChunks;

    @Value("${openai.retry.max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${openai.retry.delay-ms:1500}")
    private long retryDelayMs;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DialogueWriterService dialogueWriterService;
    private final PdfService pdfService;

    public OpenAiScriptingService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            DialogueWriterService dialogueWriterService,
            PdfService pdfService
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.dialogueWriterService = dialogueWriterService;
        this.pdfService = pdfService;
    }

    public List<String> chunkDocument(String sourceText) {
        return buildChunks(sourceText);
    }

    public ContentAnalysis analyzeContent(
            List<String> chunks,
            String requestedTitle,
            CreatePodcastRequest request
    ) {
        if (!isReady()) {
            return null;
        }

        try {
            String responseText = callResponsesApi(buildAnalysisPrompt(chunks, requestedTitle, request));
            if (responseText == null || responseText.isBlank()) {
                return null;
            }
            return parseContentAnalysisJson(responseText, requestedTitle);
        } catch (RestClientResponseException e) {
            log.error("OpenAI analysis error - Status: {}, Body: {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("OpenAI content analysis failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public ConversationPlan buildConversationPlan(
            List<String> chunks,
            ContentAnalysis analysis,
            CreatePodcastRequest request
    ) {
        if (!isReady()) {
            return null;
        }

        try {
            String responseText = callResponsesApi(buildPlanPrompt(chunks, analysis, request));
            if (responseText == null || responseText.isBlank()) {
                return null;
            }
            return parseConversationPlanJson(responseText);
        } catch (RestClientResponseException e) {
            log.error("OpenAI planning error - Status: {}, Body: {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("OpenAI conversation planning failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public String generateTranscript(
            List<String> chunks,
            ContentAnalysis analysis,
            ConversationPlan plan,
            CreatePodcastRequest request
    ) {
        if (!isReady()) {
            return dialogueWriterService.writeDialogue(analysis, plan, request);
        }

        try {
            String prompt = buildScriptPrompt(chunks, analysis, plan, request);
            String transcript = callResponsesApi(prompt);
            if (transcript == null || transcript.isBlank()) {
                throw new IllegalStateException("OpenAI response did not contain transcript text");
            }
            return transcript.trim();
        } catch (RestClientResponseException e) {
            log.error("OpenAI scripting error - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return dialogueWriterService.writeDialogue(analysis, plan, request);
        } catch (Exception e) {
            log.error("OpenAI transcript generation failed: {}", e.getMessage(), e);
            return dialogueWriterService.writeDialogue(analysis, plan, request);
        }
    }

    public boolean isReady() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public Map<String, Object> getConfigurationStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", enabled);
        status.put("ready", isReady());
        status.put("apiConfigured", apiKey != null && !apiKey.isBlank());
        status.put("model", model);
        status.put("chunkSize", chunkSize);
        status.put("maxContextChunks", maxContextChunks);
        return status;
    }

    private List<String> buildChunks(String sourceText) {
        String normalized = sourceText == null ? "" : sourceText.
                                                      replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            return new ArrayList<>();
        }

        int approximateCharChunkSize = Math.max(800, chunkSize * 4);
        List<String> chunks = pdfService.splitText(normalized, approximateCharChunkSize);
        if (chunks.size() <= maxContextChunks) {
            return chunks;
        }

        List<String> limited = new ArrayList<>();
        int headCount = Math.max(1, maxContextChunks - 2);
        for (int index = 0; index < headCount; index++) {
            limited.add(chunks.get(index));
        }
        limited.add(chunks.get(chunks.size() / 2));
        limited.add(chunks.get(chunks.size() - 1));
        return limited;
    }

    private String buildAnalysisPrompt(
            List<String> chunks,
            String requestedTitle,
            CreatePodcastRequest request
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze these document chunks and return JSON only.\n");
        prompt.append("Audience: ").append(request.getAudience()).append("\n");
        prompt.append("Difficulty: ").append(request.getDifficulty()).append("\n");
        prompt.append("Requested title: ").append(requestedTitle == null ? "" : requestedTitle).append("\n");
        prompt.append("Return exactly this JSON shape:\n");
        prompt.append("{\"title\":\"...\",\"shortSummary\":\"...\",\"sections\":[\"...\"],\"keyIdeas\":[\"...\"],\"openQuestions\":[\"...\"]}\n");
        prompt.append("Rules:\n");
        prompt.append("- sections length 3 to 6\n");
        prompt.append("- keyIdeas length 4 to 6\n");
        prompt.append("- openQuestions length 3 to 5\n");
        prompt.append("- Keep title concise.\n");
        prompt.append("- Make the summary clear and spoken-language friendly.\n\n");
        appendChunks(prompt, chunks);
        return prompt.toString();
    }

    private String buildPlanPrompt(
            List<String> chunks,
            ContentAnalysis analysis,
            CreatePodcastRequest request
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a podcast conversation plan from the analysis and document chunks. Return JSON only.\n");
        prompt.append("Return exactly this JSON shape:\n");
        prompt.append("{\"opening\":\"...\",\"beats\":[\"...\"],\"closing\":\"...\"}\n");
        prompt.append("Requirements:\n");
        prompt.append("- Audience: ").append(request.getAudience()).append("\n");
        prompt.append("- Style: ").append(request.getStyle()).append("\n");
        prompt.append("- Difficulty: ").append(request.getDifficulty()).append("\n");
        prompt.append("- Include QnA: ").append(request.isIncludeQna()).append("\n");
        prompt.append("- Include takeaways: ").append(request.isIncludeTakeaways()).append("\n");
        prompt.append("- beats length 6 to 10\n");
        prompt.append("- beats should represent conversation moves, not paragraph text\n\n");
        prompt.append("Content analysis:\n");
        prompt.append(toAnalysisJsonHint(analysis)).append("\n\n");
        appendChunks(prompt, chunks);
        return prompt.toString();
    }

    private String buildScriptPrompt(
            List<String> chunks,
            ContentAnalysis analysis,
            ConversationPlan plan,
            CreatePodcastRequest request
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are writing a polished two-speaker podcast script from a PDF document.\n");
        prompt.append("Convert dense source material into natural spoken conversation.\n\n");
        prompt.append("Speakers:\n");
        prompt.append("- ").append(request.getHostName()).append(": ").append(request.getHostRole()).append("\n");
        prompt.append("- ").append(request.getGuestName()).append(": ").append(request.getGuestRole()).append("\n\n");
        prompt.append("Constraints:\n");
        prompt.append("- Audience: ").append(request.getAudience()).append("\n");
        prompt.append("- Style: ").append(request.getStyle()).append("\n");
        prompt.append("- Difficulty: ").append(request.getDifficulty()).append("\n");
        prompt.append("- Target duration (minutes): ").append(request.getTargetDurationMinutes()).append("\n");
        prompt.append("- Every line must begin with either '").append(request.getHostName()).append(":' or '")
                .append(request.getGuestName()).append(":'.\n");
        prompt.append("- Output plain text only, no markdown, no bullet lists, no stage directions.\n");
        prompt.append("- Use the conversation plan closely.\n");
        prompt.append("- Make the conversation engaging and context-aware, not just a summary.\n\n");
        prompt.append("Content analysis:\n");
        prompt.append(toAnalysisJsonHint(analysis)).append("\n\n");
        prompt.append("Conversation plan:\n");
        prompt.append(toPlanJsonHint(plan)).append("\n\n");
        prompt.append("Reference document chunks:\n");
        appendChunks(prompt, chunks);
        return prompt.toString();
    }

    private void appendChunks(StringBuilder prompt, List<String> chunks) {
        prompt.append("Source document chunks:\n");
        for (int index = 0; index < chunks.size(); index++) {
            prompt.append("[Chunk ").append(index + 1).append("]\n");
            prompt.append(chunks.get(index)).append("\n\n");
        }
    }

    private String callResponsesApi(String prompt) throws Exception {
        Exception lastError = null;
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(apiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("model", model);
                body.put("input", prompt);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    throw new IllegalStateException("OpenAI returned an empty response");
                }

                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode outputText = root.path("output_text");
                if (outputText.isTextual() && !outputText.asText().isBlank()) {
                    return outputText.asText();
                }

                for (JsonNode item : root.path("output")) {
                    for (JsonNode content : item.path("content")) {
                        if ("output_text".equals(content.path("type").asText())) {
                            String text = content.path("text").asText();
                            if (!text.isBlank()) {
                                return text;
                            }
                        }
                    }
                }
                throw new IllegalStateException("OpenAI returned no usable text output");
            } catch (Exception e) {
                lastError = e;
                if (attempt < retryMaxAttempts) {
                    Thread.sleep(retryDelayMs * attempt);
                }
            }
        }
        throw lastError;
    }

    private ContentAnalysis parseContentAnalysisJson(String json, String requestedTitle) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        ContentAnalysis analysis = new ContentAnalysis();
        analysis.setTitle(firstNonBlank(root.path("title").asText(), requestedTitle, "Untitled Podcast"));
        analysis.setShortSummary(root.path("shortSummary").asText(""));
        analysis.setSections(readStringArray(root.path("sections")));
        analysis.setKeyIdeas(readStringArray(root.path("keyIdeas")));
        analysis.setOpenQuestions(readStringArray(root.path("openQuestions")));
        return analysis;
    }

    private ConversationPlan parseConversationPlanJson(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        ConversationPlan plan = new ConversationPlan();
        plan.setOpening(root.path("opening").asText(""));
        plan.setBeats(readStringArray(root.path("beats")));
        plan.setClosing(root.path("closing").asText(""));
        return plan;
    }

    private List<String> readStringArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            String value = item.asText().trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private String toAnalysisJsonHint(ContentAnalysis analysis) {
        return new StringBuilder()
                .append("{\"title\":\"").append(escapeJson(analysis.getTitle())).append("\",")
                .append("\"shortSummary\":\"").append(escapeJson(analysis.getShortSummary())).append("\",")
                .append("\"sections\":").append(toJsonArray(analysis.getSections())).append(",")
                .append("\"keyIdeas\":").append(toJsonArray(analysis.getKeyIdeas())).append(",")
                .append("\"openQuestions\":").append(toJsonArray(analysis.getOpenQuestions())).append("}")
                .toString();
    }

    private String toPlanJsonHint(ConversationPlan plan) {
        return new StringBuilder()
                .append("{\"opening\":\"").append(escapeJson(plan.getOpening())).append("\",")
                .append("\"beats\":").append(toJsonArray(plan.getBeats())).append(",")
                .append("\"closing\":\"").append(escapeJson(plan.getClosing())).append("\"}")
                .toString();
    }

    private String toJsonArray(List<String> values) {
        List<String> escaped = new ArrayList<>();
        for (String value : values) {
            escaped.add("\"" + escapeJson(value) + "\"");
        }
        return "[" + String.join(",", escaped) + "]";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
