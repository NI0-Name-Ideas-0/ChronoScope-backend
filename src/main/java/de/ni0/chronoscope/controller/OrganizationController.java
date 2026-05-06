package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.InviteUserRequest;
import de.ni0.chronoscope.controller.dto.response.OrganizationInvitationsResponse;
import de.ni0.chronoscope.controller.dto.response.OrganizationMembersResponse;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.OrganizationInvitationRepresentation;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Tag(name = "Organization", description = "Gather information about organizations or modify them")
@RestController
@RequestMapping("/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final RequestContext requestContext;
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
    @GetMapping("/{organizationId}/members")
    public OrganizationMembersResponse getOrganizationMembers(@PathVariable String organizationId) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityAdminOrgAccess(identity, organizationId);
        List<OrganizationMembersResponse.OrganizationMember> members = this.keycloakService.getOrganizationMembers(organizationId).stream()
                .map(m -> new OrganizationMembersResponse.OrganizationMember(
                        m.getId(), m.getUsername(), m.getFirstName(), m.getLastName(), m.getEmail()))
                .toList();
        return new OrganizationMembersResponse(members);
    }

    @DeleteMapping("/{organizationId}/members/{id}")
    public void removeMember(@PathVariable String organizationId, @PathVariable String id) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityAdminOrgAccess(identity, organizationId);
        this.keycloakService.removeMemberFromOrganization(organizationId, id);
    }

    @PostMapping("/{organizationId}/members")
    public void inviteUser(@PathVariable String organizationId, @Valid @RequestBody InviteUserRequest request) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityAdminOrgAccess(identity, organizationId);
        this.keycloakService.inviteUser(organizationId, request.targetMail());
    }

    @GetMapping("/{organizationId}/invitations")
    public OrganizationInvitationsResponse getInvitations(@PathVariable String organizationId) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityAdminOrgAccess(identity, organizationId);
        List<OrganizationInvitationsResponse.Invitation> invitations = this.keycloakService.getInvitations(organizationId)
                .stream().map(i -> new OrganizationInvitationsResponse.Invitation(
                        i.getId(), i.getFirstName(), i.getLastName(), i.getEmail(), i.getInviteLink(),
                        i.getStatus() == OrganizationInvitationRepresentation.Status.EXPIRED,
                        Instant.ofEpochSecond(i.getExpiresAt())
                )).toList();
        return new OrganizationInvitationsResponse(invitations);
    }

    @DeleteMapping("/{organizationId}/invitations/{invitationId}")
    public void deleteInvitation(@PathVariable String organizationId, @PathVariable String invitationId) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityAdminOrgAccess(identity, organizationId);
        this.keycloakService.deleteInvitation(organizationId, invitationId);
    }

    @PutMapping("/{organizationId}/invitations/{invitationId}")
    public void resendInvitation(@PathVariable String organizationId, @PathVariable String invitationId) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityAdminOrgAccess(identity, organizationId);
        this.keycloakService.resendInvitation(organizationId, invitationId);
    }
}
