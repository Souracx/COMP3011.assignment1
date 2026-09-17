package comp3011.assignment1.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import comp3011.assignment1.model.ErrorResponse;

/**
 * Makes every error from every endpoint use the spec's ErrorResponse shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Every framework exception handled by the parent class ends up here. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {

        String message = ex.getMessage();
        if (ex instanceof org.springframework.web.ErrorResponse springError
                && springError.getBody().getDetail() != null) {
            message = springError.getBody().getDetail();
        }
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        return ResponseEntity.status(statusCode)
                .headers(headers)
                .body(build(statusCode, message, path));
    }

    /** A POST that isn't multipart at all is the client's fault, not a 500. */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleBadMultipart(
            MultipartException ex, jakarta.servlet.http.HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(build(status, "Request must be multipart/form-data with an 'audio' part.",
                        request.getRequestURI()));
    }

    /** Catch-all: log the real cause server-side, send a safe message. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(build(status, "An unexpected error occurred.", request.getRequestURI()));
    }

    private static ErrorResponse build(HttpStatusCode statusCode, String message, String path) {
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        String reason = (resolved != null) ? resolved.getReasonPhrase() : "Error";
        return new ErrorResponse(
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                statusCode.value(),
                reason,
                message,
                path);
    }
}