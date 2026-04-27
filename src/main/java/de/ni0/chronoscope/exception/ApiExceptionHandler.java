package de.ni0.chronoscope.exception;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException exception, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.ACCOUNT_NOT_FOUND,
                "Account Not Found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNoResourceFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.RESOURCE_NOT_FOUND,
                "Not Found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ProblemDetail handleMissingResource(Exception exception, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.RESOURCE_NOT_FOUND,
                "Not Found",
                "The requested resource was not found",
                request
        );
    }

    @ExceptionHandler(AccountAccessDeniedException.class)
    public ProblemDetail handleAccountAccessDenied(AccountAccessDeniedException exception, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.FORBIDDEN,
                ApiErrorCode.ACCESS_DENIED,
                "Access Denied",
                exception.getMessage(),
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

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException exception, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                "Validation Failed",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected exception on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(Throwable.class)
    public ProblemDetail handleUnexpectedError(Throwable throwable, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), throwable);
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                throwable.getMessage(),
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
