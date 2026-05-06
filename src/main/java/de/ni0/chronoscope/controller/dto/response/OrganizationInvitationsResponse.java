package de.ni0.chronoscope.controller.dto.response;

import java.time.Instant;
import java.util.List;

public record OrganizationInvitationsResponse(
        List<Invitation> invitations
) {
    public record Invitation(
       String id,
       String firstName,
       String lastName,
       String email,
       String inviteLink,
       boolean expired,
       Instant expiresAt
    ){}
}
