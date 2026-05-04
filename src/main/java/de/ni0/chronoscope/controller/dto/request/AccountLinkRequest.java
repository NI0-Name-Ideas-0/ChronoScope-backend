package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for sending an account-link invitation to another account e-mail.
 */
public record AccountLinkRequest(
    @NotBlank @Email String targetEmail
) {
}
