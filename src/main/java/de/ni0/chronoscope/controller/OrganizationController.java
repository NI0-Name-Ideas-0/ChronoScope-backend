package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.InviteUserRequest;
import de.ni0.chronoscope.controller.dto.response.OrganizationInvitationsResponse;
import de.ni0.chronoscope.controller.dto.response.OrganizationMembersResponse;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.OrganizationInvitationRepresentation;
import org.springframework.http.HttpStatus;
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
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Not an admin of the organization", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
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

    @Operation(summary = "Remove organization member", description = "Removes a member from the organization. The identity must be an admin of the organization.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Not an admin of the organization", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{organizationId}/members/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@Parameter(description = "Organization ID") @PathVariable String organizationId,
                             @Parameter(description = "User ID to remove") @PathVariable String id) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityAdminOrgAccess(identity, organizationId);
        this.keycloakService.removeMemberFromOrganization(organizationId, id);
    }

    @Operation(summary = "Invite user to organization", description = "Sends an invitation email to the given address. The identity must be an admin of the organization.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Invitation sent"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Not an admin of the organization", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{organizationId}/members")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void inviteUser(@Parameter(description = "Organization ID") @PathVariable String organizationId,
                           @Valid @RequestBody InviteUserRequest request) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityAdminOrgAccess(identity, organizationId);
        this.keycloakService.inviteUser(organizationId, request.targetMail());
    }

    @Operation(summary = "List organization invitations", description = "Returns all pending invitations for the organization. The identity must be an admin of the organization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Not an admin of the organization", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{organizationId}/invitations")
    public OrganizationInvitationsResponse getInvitations(@Parameter(description = "Organization ID") @PathVariable String organizationId) {
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

    @Operation(summary = "Delete organization invitation", description = "Deletes a pending invitation. The identity must be an admin of the organization.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Invitation deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Not an admin of the organization", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{organizationId}/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvitation(@Parameter(description = "Organization ID") @PathVariable String organizationId,
                                 @Parameter(description = "Invitation ID") @PathVariable String invitationId) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityAdminOrgAccess(identity, organizationId);
        this.keycloakService.deleteInvitation(organizationId, invitationId);
    }

    @Operation(summary = "Resend organization invitation", description = "Resends an existing invitation email. The identity must be an admin of the organization.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Invitation resent"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Not an admin of the organization", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{organizationId}/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendInvitation(@Parameter(description = "Organization ID") @PathVariable String organizationId,
                                 @Parameter(description = "Invitation ID") @PathVariable String invitationId) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityAdminOrgAccess(identity, organizationId);
        this.keycloakService.resendInvitation(organizationId, invitationId);
    }
}
