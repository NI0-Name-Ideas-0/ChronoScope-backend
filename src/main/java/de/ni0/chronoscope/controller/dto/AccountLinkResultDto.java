package de.ni0.chronoscope.controller.dto;

public record AccountLinkResultDto(
    Long sourceAccountId,
    Long targetAccountId,
    String status
) {
}
