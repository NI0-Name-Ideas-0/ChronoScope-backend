package de.ni0.chronoscope.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiNotImplementedException.class)
    public ProblemDetail handleApiNotImplemented(ApiNotImplementedException exception, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.NOT_IMPLEMENTED,
                ApiErrorCode.API_NOT_IMPLEMENTED,
                "Not Implemented",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InsufficientSlotsException.class)
    public ProblemDetail handleInsufficientSlots(InsufficientSlotsException exception, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.CONFLICT,
                ApiErrorCode.INSUFFICIENT_SLOTS,
                "Planning Failed",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException exception, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.RESOURCE_NOT_FOUND,
                "Not Found",
                "The requested resource was not found",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                "Validation Failed",
                "Request validation failed",
                request
        );
        List<ValidationFieldError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toValidationFieldError)
                .toList();
        problemDetail.setProperty("fieldErrors", fieldErrors);
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred",
                request
        );
    }

    private ProblemDetail createProblemDetail(
            HttpStatus status,
            ApiErrorCode errorCode,
            String title,
            String detail,
            HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(errorCode.type());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("errorCode", errorCode.name());
        return problemDetail;
    }

    private ValidationFieldError toValidationFieldError(FieldError fieldError) {
        return new ValidationFieldError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private record ValidationFieldError(String field, String message) {
    }
}
