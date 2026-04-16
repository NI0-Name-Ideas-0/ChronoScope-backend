package de.ni0.chronoscope.controller.dto.response;

import de.ni0.chronoscope.controller.dto.AccountDto;

import java.util.List;

public record IdentityResponse(
    Long id,
    List<AccountDto> accounts
) {
}
