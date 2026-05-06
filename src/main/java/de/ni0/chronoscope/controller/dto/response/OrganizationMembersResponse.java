package de.ni0.chronoscope.controller.dto.response;

import java.util.List;

public record OrganizationMembersResponse(
    List<OrganizationMember> members
) {

    public record OrganizationMember(
            String id,
            String userName,
            String firstName,
            String lastName,
            String email
    ) {}
}
