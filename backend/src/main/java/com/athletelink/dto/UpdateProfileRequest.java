package com.athletelink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String fullName;

    private String sport;
    private String location;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
