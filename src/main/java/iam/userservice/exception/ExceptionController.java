package iam.userservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.DateTimeException;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class ExceptionController extends ResponseEntityExceptionHandler {

    public static final String INVALID_REQUEST_ARGUMENT = "Invalid request argument";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIError> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        APIError errorDetails = new APIError(ex.getMessage(),
                extractPath(request.getDescription(false)));
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<APIError> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex, WebRequest request) {
        APIError errorDetails = new APIError(ex.getMessage(),
                extractPath(request.getDescription(false)));
        return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
    }

    /* This exception is thrown when a client sends a request with an illegal request argument e.g. invalid date format, negative value, etc. */
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            DateTimeException.class})
    public ResponseEntity<Object> handleIllegalArgument(RuntimeException ex, WebRequest request) {
        log.error("Illegal exception: {} thrown", ex.getMessage());
        APIError apiError = new APIError(
                INVALID_REQUEST_ARGUMENT, extractPath(request.getDescription(false)));
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, WebRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();
        String errorString = String.join(", ", errors);
        APIError apiError = new APIError(errorString, extractPath(request.getDescription(false)));
        return handleExceptionInternal(ex, apiError, headers, status, request);
    }

    /* This exception is thrown when a requested resource cannot be located e.g. user enters an invalid URL path. */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, WebRequest request) {
        APIError apiError = new APIError(
                ex.getLocalizedMessage(), extractPath(request.getDescription(false)));
        return handleExceptionInternal(ex, apiError, headers, status, request);
    }

    /* This exception handles missing or unexpected required content type e.g. request requires 'application/json and receives 'text' */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, WebRequest request) {
        APIError apiError = new APIError(
                ex.getLocalizedMessage(),
                extractPath(request.getDescription(false)));
        return handleExceptionInternal(ex, apiError, headers, status, request);
    }

    /*    This exception is thrown when a client sends a request with an unsupported HTTP method e.g. PUT, DELETE, etc. */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, WebRequest request) {
        APIError apiError = new APIError(
                ex.getLocalizedMessage(),
                extractPath(request.getDescription(false)));
        return handleExceptionInternal(ex, apiError, headers, status, request);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> fallBack(Exception ex, WebRequest request) {
        APIError apiError = new APIError(
                ex.getLocalizedMessage(), extractPath(request.getDescription(false)));
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String extractPath(String originalPath) {
        // Assuming "uri=" is always present, find its position and extract the path by removing "uri="
        int uriIndex = originalPath.indexOf("uri=");
        if (uriIndex != -1) {
            return originalPath.substring(uriIndex + 4);
        } else {
            return originalPath;
        }
    }

}
