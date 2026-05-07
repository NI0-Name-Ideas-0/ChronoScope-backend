package de.ni0.chronoscope.controller.dto.response;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrganizationMembersResponse(
    @NotNull List<OrganizationMember> members
) {

    public record OrganizationMember(
            @NotNull String id,
            @NotNull String userName,
            @NotNull String firstName,
            @NotNull String lastName,
            @NotNull String email
    ) {}
}
