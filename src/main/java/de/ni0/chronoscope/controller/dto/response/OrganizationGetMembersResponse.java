package de.ni0.chronoscope.controller.dto.response;

import java.util.List;

public record OrganizationGetMembersResponse(
    List<OrganizationMember> members
) {

    public record OrganizationMember(
            String id,
            String name,
            String email
    ) {}
}
