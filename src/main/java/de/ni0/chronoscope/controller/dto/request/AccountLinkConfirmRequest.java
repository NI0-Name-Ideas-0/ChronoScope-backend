package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload containing the one-time token used to confirm account linking.
 */
public record AccountLinkConfirmRequest(
    @NotBlank String token
) {
}
