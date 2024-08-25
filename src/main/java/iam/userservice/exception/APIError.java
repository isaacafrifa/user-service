package iam.userservice.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record APIError(
        String message,
        String path,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timeStamp) {

    public APIError(String message, String path) {
        this(message, path, LocalDateTime.now());
    }
}
