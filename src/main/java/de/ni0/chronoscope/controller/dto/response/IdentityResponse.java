package de.ni0.chronoscope.controller.dto.response;

import java.util.List;

public record IdentityResponse(
    Long id,
    List<AccountResponse> accounts
) {
}
