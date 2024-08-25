package iam.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserOptimisticLockException extends RuntimeException {
    public UserOptimisticLockException(String message) {
        super(message);
    }
}
