package com.athletelink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AthleteMetricRequest {
    @NotBlank(message = "Metric name is required (e.g. 100m Sprint, Vertical Jump)")
    private String metricName;

    @NotNull(message = "Metric value is required")
    private BigDecimal metricValue;

    @NotBlank(message = "Unit is required (e.g. seconds, cm, kg, km/h)")
    private String unit;

    private String category; // e.g. Speed, Strength, Endurance, Sport-Specific
    private String recordedDate; // YYYY-MM-DD
    private String notes; // optional context (e.g. State Trial, Practice)

    public AthleteMetricRequest() {}

    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }

    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRecordedDate() { return recordedDate; }
    public void setRecordedDate(String recordedDate) { this.recordedDate = recordedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
