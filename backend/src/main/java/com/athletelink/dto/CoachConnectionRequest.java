package com.athletelink.dto;

import jakarta.validation.constraints.Size;

public class CoachConnectionRequest {
    @Size(max=500, message="Message must be 500 characters or less")
    private String message;
    public String getMessage(){ return message; }
    public void setMessage(String message){ this.message=message; }
}
