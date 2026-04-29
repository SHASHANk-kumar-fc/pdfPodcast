package com.pdfpodcast.service;

import com.pdfpodcast.model.ContentAnalysis;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ContentAnalysisService {

    public ContentAnalysis analyze(String text, String requestedTitle) {
        String normalizedText = normalizeText(text);
        List<String> sentences = splitSentences(normalizedText);

        ContentAnalysis analysis = new ContentAnalysis();
        analysis.setTitle(resolveTitle(requestedTitle, sentences));
        analysis.setShortSummary(sentences.isEmpty() ? "" : sentences.get(0));
        analysis.setSections(extractSections(normalizedText));
        analysis.setKeyIdeas(extractKeyIdeas(sentences));
        analysis.setOpenQuestions(buildOpenQuestions(analysis.getKeyIdeas()));
        return analysis;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private String resolveTitle(String requestedTitle, List<String> sentences) {
        if (requestedTitle != null && !requestedTitle.isBlank()) {
            return requestedTitle.trim();
        }
        if (!sentences.isEmpty()) {
            String first = sentences.get(0);
            return first.length() > 72 ? first.substring(0, 72).trim() + "..." : first;
        }
        return "Untitled Podcast";
    }

    private List<String> extractSections(String text) {
        Set<String> sections = new LinkedHashSet<>();
        for (String chunk : text.split("(?i)(?:introduction|abstract|conclusion|references|methodology|results)")) {
            String cleaned = chunk.trim();
            if (cleaned.length() > 30) {
                sections.add(cleaned.substring(0, Math.min(cleaned.length(), 60)).trim());
            }
            if (sections.size() == 4) {
                break;
            }
        }
        return new ArrayList<>(sections);
    }

    private List<String> extractKeyIdeas(List<String> sentences) {
        List<String> ideas = new ArrayList<>();
        for (String sentence : sentences) {
            if (sentence.length() < 40) {
                continue;
            }
            ideas.add(sentence);
            if (ideas.size() == 5) {
                break;
            }
        }
        return ideas;
    }

    private List<String> buildOpenQuestions(List<String> keyIdeas) {
        List<String> questions = new ArrayList<>();
        for (String idea : keyIdeas) {
            questions.add("Why does this matter in practice: " + trimForPrompt(idea) + "?");
            if (questions.size() == 3) {
                break;
            }
        }
        return questions;
    }

    private String trimForPrompt(String text) {
        return text.length() > 70 ? text.substring(0, 70).trim() + "..." : text;
    }

    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        if (text.isBlank()) {
            return sentences;
        }
        for (String sentence : text.split("(?<=[.!?])\\s+")) {
            String cleaned = sentence.trim();
            if (!cleaned.isEmpty()) {
                sentences.add(cleaned);
            }
        }
        return sentences;
    }
}
