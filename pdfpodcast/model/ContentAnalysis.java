package com.pdfpodcast.model;

import java.util.ArrayList;
import java.util.List;

public class ContentAnalysis {

    private String title;
    private String shortSummary;
    private List<String> sections = new ArrayList<>();
    private List<String> keyIdeas = new ArrayList<>();
    private List<String> openQuestions = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortSummary() {
        return shortSummary;
    }

    public void setShortSummary(String shortSummary) {
        this.shortSummary = shortSummary;
    }

    public List<String> getSections() {
        return sections;
    }

    public void setSections(List<String> sections) {
        this.sections = sections;
    }

    public List<String> getKeyIdeas() {
        return keyIdeas;
    }

    public void setKeyIdeas(List<String> keyIdeas) {
        this.keyIdeas = keyIdeas;
    }

    public List<String> getOpenQuestions() {
        return openQuestions;
    }

    public void setOpenQuestions(List<String> openQuestions) {
        this.openQuestions = openQuestions;
    }
}
