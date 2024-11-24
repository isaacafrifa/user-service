package iam.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EventPublishingException extends RuntimeException {
    public EventPublishingException(String message) {
        super(message);
    }
}
