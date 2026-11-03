package com.athletelink.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ApiErrorResponse {

    private LocalDateTime timestamp = LocalDateTime.now();
    private int status;
    private String message;
    private List<String> details;

    public ApiErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public ApiErrorResponse(int status, String message, List<String> details) {
        this.status = status;
        this.message = message;
        this.details = details;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public List<String> getDetails() { return details; }
}
