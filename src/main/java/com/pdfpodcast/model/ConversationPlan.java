package com.pdfpodcast.model;

import java.util.ArrayList;
import java.util.List;

public class ConversationPlan {

    private String opening;
    private List<String> beats = new ArrayList<>();
    private String closing;

    public String getOpening() {
        return opening;
    }

    public void setOpening(String opening) {
        this.opening = opening;
    }

    public List<String> getBeats() {
        return beats;
    }

    public void setBeats(List<String> beats) {
        this.beats = beats;
    }

    public String getClosing() {
        return closing;
    }

    public void setClosing(String closing) {
        this.closing = closing;
    }
}
