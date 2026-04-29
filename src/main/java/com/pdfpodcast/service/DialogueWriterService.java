package com.pdfpodcast.service;

import com.pdfpodcast.model.ContentAnalysis;
import com.pdfpodcast.model.ConversationPlan;
import com.pdfpodcast.model.CreatePodcastRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DialogueWriterService {

    public String writeDialogue(ContentAnalysis analysis, ConversationPlan plan, CreatePodcastRequest request) {
        List<String> turns = new ArrayList<>();
        String host = request.getHostName();
        String guest = request.getGuestName();

        turns.add(host + ": Welcome back. Today we're unpacking " + analysis.getTitle() + " for " + request.getAudience() + ".");
        turns.add(guest + ": I'll help translate the core ideas into a " + request.getStyle() + ", easy-to-follow conversation.");

        for (String beat : plan.getBeats()) {
            if (beat.startsWith("Explain: ")) {
                String idea = beat.substring("Explain: ".length());
                turns.add(host + ": Let's start with a central idea. " + toQuestion(idea, request.getDifficulty()));
                turns.add(guest + ": " + rewriteForSpeech(idea, request));
            } else if (beat.startsWith("Why does this matter in practice: ")) {
                String prompt = beat.substring("Why does this matter in practice: ".length());
                turns.add(host + ": " + prompt);
                turns.add(guest + ": In practice, this changes how people make decisions, because it turns abstract theory " +
                        "into something measurable and useful.");
            } else {
                turns.add(host + ": Before we close, what should listeners remember?");
                turns.add(guest + ": The key takeaway is to focus on the main mechanism, ignore the noise, and connect the" +
                        " evidence back to real-world use.");
            }
        }

        turns.add(host + ": Give us one closing thought.");
        turns.add(guest + ": " + closingLine());
        return String.join("\n", turns);
    }

    private String rewriteForSpeech(String idea, CreatePodcastRequest request) {
        String rewritten;
        if ("beginner".equalsIgnoreCase(request.getDifficulty())) {
            rewritten = "In simple terms, " + decapitalize(idea);
        } else if ("advanced".equalsIgnoreCase(request.getDifficulty())) {
            rewritten = "At a deeper level, " + decapitalize(idea) + " This matters because the assumptions and tradeoffs " +
                    "are part of the story.";
        } else {
            rewritten = "The useful way to think about it is this: " + decapitalize(idea);
        }

        if ("storytelling".equalsIgnoreCase(request.getStyle())) {
            rewritten += " Think of it like a case study that reveals the logic step by step.";
        } else if ("concise".equalsIgnoreCase(request.getStyle())) {
            rewritten += " The short version is that the idea is important, actionable, and worth remembering.";
        } else {
            rewritten += " The conversation format makes the argument easier to absorb than a dense paper.";
        }

        return rewritten;
    }

    private String toQuestion(String idea, String difficulty) {
        if ("advanced".equalsIgnoreCase(difficulty)) {
            return "What is the core mechanism behind this claim: " + trim(idea) + "?";
        }
        return "What does this really mean: " + trim(idea) + "?";
    }

    private String closingLine() {
        return "If you tailor the explanation to the audience, choose the right level " +
                "of depth, and keep the structure conversational, the material becomes far more engaging.";
    }

    private String trim(String text) {
        return text.length() > 110 ? text.substring(0, 110).trim() + "..." : text;
    }

    private String decapitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }
}
