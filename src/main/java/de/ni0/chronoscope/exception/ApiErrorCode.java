package de.ni0.chronoscope.exception;

import java.net.URI;
import java.util.Locale;

/**
 * Stable application error codes exposed in RFC 9457 problem details.
 */
public enum ApiErrorCode {
    API_NOT_IMPLEMENTED,
    INSUFFICIENT_SLOTS,
    RESOURCE_NOT_FOUND,
    VALIDATION_ERROR,
    INVALID_REQUEST,
    ACCESS_DENIED,
    ACCOUNT_NOT_FOUND,
    INTERNAL_SERVER_ERROR;

    /**
     * Converts the error code to the URN used as {@link org.springframework.http.ProblemDetail#getType()}.
     *
     * @return problem-detail type URI
     */
    public URI type() {
        return URI.create("urn:chronoscope:error:" + name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }
}
