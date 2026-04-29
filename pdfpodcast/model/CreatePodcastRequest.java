package com.pdfpodcast.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public class CreatePodcastRequest {

    private MultipartFile file;
    private String title;
    @NotBlank
    private String audience = "curious professionals";
    @NotBlank
    private String style = "engaging";
    @NotBlank
    private String difficulty = "intermediate";
    @Min(3)
    @Max(60)
    private Integer targetDurationMinutes = 8;
    @NotBlank
    private String hostName = "Maya";
    @NotBlank
    private String guestName = "Dr. Chen";
    @NotBlank
    private String hostRole = "thoughtful host";
    @NotBlank
    private String guestRole = "subject matter expert";
    private boolean includeQna = true;
    private boolean includeTakeaways = true;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Integer getTargetDurationMinutes() {
        return targetDurationMinutes;
    }

    public void setTargetDurationMinutes(Integer targetDurationMinutes) {
        this.targetDurationMinutes = targetDurationMinutes;
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
}
