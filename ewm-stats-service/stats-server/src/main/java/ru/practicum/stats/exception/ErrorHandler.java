package ru.practicum.stats.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(BadRequestException e) {
        log.warn("Bad request: {}", e.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> "Field: " + err.getField() + ". Error: " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");
        log.warn("Validation error: {}", message);
        return buildError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: {}", e.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleAny(Throwable e) {
        log.error("Unexpected error", e);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error.", e.getMessage());
    }

    private Map<String, Object> buildError(HttpStatus status, String reason, String message) {
        return Map.of(
                "status", status.name(),
                "reason", reason,
                "message", message != null ? message : "",
                "timestamp", LocalDateTime.now().format(FORMATTER)
        );
    }
}
