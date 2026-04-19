package de.ni0.chronoscope.controller.dto.response;

public record AccountLinkConfirmResponse(
    Long sourceAccountId,
    Long targetAccountId,
    String status
) {
}
