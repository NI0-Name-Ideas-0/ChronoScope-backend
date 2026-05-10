package de.ni0.chronoscope.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.representations.idm.OrganizationRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.AccountLinkConfirmRequest;
import de.ni0.chronoscope.controller.dto.request.AccountLinkRequest;
import de.ni0.chronoscope.controller.dto.request.SettingsUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.AccountLinkConfirmResponse;
import de.ni0.chronoscope.controller.dto.response.AccountResponse;
import de.ni0.chronoscope.controller.dto.response.IdentityResponse;
import de.ni0.chronoscope.controller.dto.response.SettingsResponse;
import de.ni0.chronoscope.mapper.IdentityMapper;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.service.IdentityService;
import de.ni0.chronoscope.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for identity operations.
 *
 * <p>Exposes the current authenticated identity and account linking endpoints.
 */
@Tag(name = "Identity", description = "Retrieve the current identity and manage account linking")
@RestController
@RequestMapping("/v1/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;
    private final RequestContext requestContext;
    private final IdentityMapper identityMapper;
    private final KeycloakService keycloakService;

    /**
     * Retrieves the authenticated identity.
     *
     * @return the identity response including linked accounts
     */
    @Operation(summary = "Get current identity", description = "Retrieve information about the authenticated identity, including all linked accounts and the organizations for which the identity has admin privileges, as resolved server-side.")    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Identity retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public IdentityResponse getIdentity() {
        Identity identity = requestContext.getAccount().getIdentity();
        Set<String> adminOrganizations = this.keycloakService.getAdminOrganizations(identity);
        Set<OrganizationRepresentation> organizations = this.keycloakService.getIdentityOrganizations(identity);
        List<IdentityResponse.Organization> orgs = new java.util.ArrayList<>(organizations.stream().map(o ->
                new IdentityResponse.Organization(o.getName(), o.getId())).toList());
        orgs.add(new IdentityResponse.Organization("Privat", "private"));

        IdentityResponse response = this.identityMapper.toResponse(
                identity,
                adminOrganizations,
                new HashSet<>(orgs)
        );

        Map<Long, String> idMapping = new HashMap<>();
        for (Account account : identity.getAccounts()) {
            idMapping.put(account.getId(), account.getSubject());
        }
        List<AccountResponse> accountResponses = new ArrayList<>();
        for (AccountResponse account : response.accounts()) {
            String mail = this.keycloakService.getMail(idMapping.get(account.id()));
            accountResponses.add(new AccountResponse(account.id(), account.identityId(), mail));
        }

        return new IdentityResponse(
            response.id(),
            accountResponses,
            response.adminOrganizations(),
            response.organizations()
        );
    }

    @GetMapping("/settings")
    public de.ni0.chronoscope.controller.dto.response.SettingsResponse getSettings() {
        long identityId = this.requestContext.getAccount().getIdentity().getId();
        var settings = this.identityService.getSettings(identityId);
        return new de.ni0.chronoscope.controller.dto.response.SettingsResponse(
            settings.getLanguage(), settings.getTheme(), settings.getWorkSettings()
        );
    }

    /**
     * Requests an account link for the authenticated identity.
     *
     * @param request the account link request payload
     */
    @Operation(summary = "Request account linking", description = "Send a confirmation link to the target e-mail address. If the target accepts, the two accounts' identities are merged.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Link e-mail sent"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestAccountLink(@Valid @RequestBody AccountLinkRequest request) {
        long accountId = this.requestContext.getAccount().getId();
        String targetEmail = request.targetEmail();
        this.identityService.sendLink(accountId, targetEmail);
    }

    /**
     * Confirms an account-link token and merges the target account's identity into the source identity.
     *
     * @param request the confirmation token payload
     * @return IDs of the accounts involved in the merge
     */
    @Operation(summary = "Confirm account linking", description = "Confirm a pending account link using the token received via e-mail. Returns the IDs of both linked accounts.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account link confirmed"),
        @ApiResponse(responseCode = "400", description = "Validation error or invalid/expired token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/accounts/confirm")
    public AccountLinkConfirmResponse confirmAccountLink(@Valid @RequestBody AccountLinkConfirmRequest request) {
        long identityId = this.requestContext.getAccount().getIdentity().getId();
        return this.identityService.mergeAccounts(identityId, request.token());
    }

    /**
     * Updates the authenticated identity's language and/or theme settings.
     *
     * @param request the settings update request with optional language and theme
     * @return the updated settings response
     */
    @Operation(summary = "Update identity settings", description = "Update language and/or theme preferences for the authenticated identity. Both fields are optional; only specified fields will be updated.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Settings updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/settings")
    public SettingsResponse updateSettings(@Valid @RequestBody SettingsUpdateRequest request) {
        long identityId = this.requestContext.getAccount().getIdentity().getId();
        this.identityService.updateSettings(identityId, request);
        return this.getSettings();
    }
}
