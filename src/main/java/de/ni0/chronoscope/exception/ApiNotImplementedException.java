package de.ni0.chronoscope.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
public class ApiNotImplementedException extends RuntimeException {
    public ApiNotImplementedException() {
        super("Not implemented yet");
    }
}
