package com.example.Escolar.Dto;

import java.time.LocalDateTime;
import java.util.List;

public class BatchDeleteErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private List<ItemDeleteError> items;

    public BatchDeleteErrorResponse(LocalDateTime timestamp, int status, String error, String message, List<ItemDeleteError> items) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.items = items;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public List<ItemDeleteError> getItems() {
        return items;
    }
}
