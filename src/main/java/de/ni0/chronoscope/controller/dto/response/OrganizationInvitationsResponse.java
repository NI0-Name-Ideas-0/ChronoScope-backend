package de.ni0.chronoscope.controller.dto.response;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record OrganizationInvitationsResponse(
    @NotNull List<Invitation> invitations
) {
    public record Invitation(
        @NotNull String id,
        @NotNull String firstName,
        @NotNull String lastName,
        @NotNull String email,
        @NotNull String inviteLink,
        @NotNull boolean expired,
        @NotNull Instant expiresAt
    ){}
}
