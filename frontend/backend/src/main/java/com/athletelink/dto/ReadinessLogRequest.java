package com.athletelink.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ReadinessLogRequest {
    @NotNull(message = "Resting heart rate is required")
    @Min(value = 35, message = "Resting HR must be at least 35 bpm")
    @Max(value = 130, message = "Resting HR must be realistic")
    private Integer restingHr;

    @NotNull(message = "Sleep hours is required")
    @Positive(message = "Sleep hours must be positive")
    @Max(value = 16, message = "Sleep hours cannot exceed 16")
    private Double sleepHours;

    @NotNull(message = "Soreness level is required (1-10)")
    @Min(value = 1, message = "Soreness level minimum is 1")
    @Max(value = 10, message = "Soreness level maximum is 10")
    private Integer sorenessLevel;

    @NotNull(message = "Fatigue level is required (1-10)")
    @Min(value = 1, message = "Fatigue level minimum is 1")
    @Max(value = 10, message = "Fatigue level maximum is 10")
    private Integer fatigueLevel;

    @NotNull(message = "Hydration is required")
    @Positive(message = "Hydration must be positive")
    private Double hydrationLiters;

    private String notes;

    public ReadinessLogRequest() {}

    public Integer getRestingHr() { return restingHr; }
    public void setRestingHr(Integer restingHr) { this.restingHr = restingHr; }

    public Double getSleepHours() { return sleepHours; }
    public void setSleepHours(Double sleepHours) { this.sleepHours = sleepHours; }

    public Integer getSorenessLevel() { return sorenessLevel; }
    public void setSorenessLevel(Integer sorenessLevel) { this.sorenessLevel = sorenessLevel; }

    public Integer getFatigueLevel() { return fatigueLevel; }
    public void setFatigueLevel(Integer fatigueLevel) { this.fatigueLevel = fatigueLevel; }

    public Double getHydrationLiters() { return hydrationLiters; }
    public void setHydrationLiters(Double hydrationLiters) { this.hydrationLiters = hydrationLiters; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
