package com.pdfpodcast.service;

import com.pdfpodcast.model.ContentAnalysis;
import com.pdfpodcast.model.ConversationPlan;
import com.pdfpodcast.model.CreatePodcastRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationPlannerService {

    public ConversationPlan buildPlan(ContentAnalysis analysis, CreatePodcastRequest request) {
        ConversationPlan plan = new ConversationPlan();
        plan.setOpening("Introduce the topic '" + analysis.getTitle() + "' for " + request.getAudience() + " in a " + request.getStyle()
                + " tone.");

        List<String> beats = new ArrayList<>();
        for (String keyIdea : analysis.getKeyIdeas()) {
            beats.add("Explain: " + keyIdea);
        }
        if (request.isIncludeQna()) {
            beats.addAll(analysis.getOpenQuestions());
        }
        if (request.isIncludeTakeaways()) {
            beats.add("Close with practical takeaways at " + request.getDifficulty() + " difficulty.");
        }
        plan.setBeats(beats);
        plan.setClosing("Wrap up with one memorable insight and invite the listener to continue exploring the topic.");
        return plan;
    }
}
