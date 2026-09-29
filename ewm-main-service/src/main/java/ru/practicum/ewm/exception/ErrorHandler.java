package ru.practicum.ewm.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.ewm.dto.ApiError;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException e) {
        log.warn("Not found: {}", e.getMessage());
        return buildError(HttpStatus.NOT_FOUND, "The required object was not found.", e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException e) {
        log.warn("Conflict: {}", e.getMessage());
        return buildError(HttpStatus.CONFLICT, "For the requested operation the conditions are not met.", e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(BadRequestException e) {
        log.warn("Bad request: {}", e.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(err -> "Field: " + err.getField() + ". Error: " + err.getDefaultMessage()
                        + ". Value: " + err.getRejectedValue())
                .toList();
        String message = errors.isEmpty() ? "Validation error" : errors.get(0);
        log.warn("Validation error: {}", message);
        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", message);
        apiError.setErrors(errors);
        return apiError;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: {}", e.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: {}", e.getMessage());
        String message = String.format(
                "Failed to convert value of type '%s' to required type '%s'; nested exception is %s",
                e.getValue(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown",
                e.getName());
        return buildError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadJson(HttpMessageNotReadableException e) {
        log.warn("Malformed JSON: {}", e.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleAny(Exception e) {
        log.error("Unexpected error", e);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error.", e.getMessage());
    }

    private ApiError buildError(HttpStatus status, String reason, String message) {
        return ApiError.builder()
                .status(status.name())
                .reason(reason)
                .message(message != null ? message : "")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
