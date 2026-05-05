package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.request.OrganizationGetMembersRequest;
import de.ni0.chronoscope.controller.dto.response.OrganizationGetMembersResponse;
import de.ni0.chronoscope.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Organization", description = "Gather information about organizations or modify them")
@RestController
@RequestMapping("/v1/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final KeycloakService keycloakService;

    /**
     * Retrieves members of a specific organization.
     * The identity must be an admin of the organization
     */
    @Operation(summary = "Get Organization members", description = "Retrieves members of a specific organization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Members retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public OrganizationGetMembersResponse getOrganizationMembers(@Valid @RequestBody OrganizationGetMembersRequest request) {
        List<OrganizationGetMembersResponse.OrganizationMember> members = this.keycloakService.getOrganizationMembers(request.organization()).stream()
                .map(m -> new OrganizationGetMembersResponse.OrganizationMember(m.getId(), m.getUsername(), m.getEmail()))
                .toList();
        return new OrganizationGetMembersResponse(members);
    }
}
