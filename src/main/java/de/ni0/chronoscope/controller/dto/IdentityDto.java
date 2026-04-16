package de.ni0.chronoscope.controller.dto;

import java.util.List;

public record IdentityDto(
    Long id,
    List<AccountDto> accounts
) {
}
