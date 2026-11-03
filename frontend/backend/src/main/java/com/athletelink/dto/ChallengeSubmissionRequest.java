package com.athletelink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ChallengeSubmissionRequest {
    @NotBlank(message = "Drill type is required")
    private String drillType;

    @NotBlank(message = "Drill name is required")
    private String drillName;

    @NotNull(message = "Score is required")
    @Positive(message = "Score must be positive")
    private Double score;

    @NotBlank(message = "Unit is required")
    private String unit;

    private String videoUrl;
    private String notes;

    public ChallengeSubmissionRequest() {}

    public String getDrillType() { return drillType; }
    public void setDrillType(String drillType) { this.drillType = drillType; }

    public String getDrillName() { return drillName; }
    public void setDrillName(String drillName) { this.drillName = drillName; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
