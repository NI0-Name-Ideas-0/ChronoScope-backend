package de.ni0.chronoscope.exception;

import java.net.URI;
import java.util.Locale;

public enum ApiErrorCode {
    API_NOT_IMPLEMENTED,
    INSUFFICIENT_SLOTS,
    RESOURCE_NOT_FOUND,
    VALIDATION_ERROR,
    INVALID_REQUEST,
    ACCESS_DENIED,
    ACCOUNT_NOT_FOUND,
    INTERNAL_SERVER_ERROR;

    public URI type() {
        return URI.create("urn:chronoscope:error:" + name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }
}
