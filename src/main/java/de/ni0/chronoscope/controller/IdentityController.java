package de.ni0.chronoscope.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.AccountLinkConfirmRequest;
import de.ni0.chronoscope.controller.dto.request.AccountLinkRequest;
import de.ni0.chronoscope.controller.dto.response.AccountLinkConfirmResponse;
import de.ni0.chronoscope.controller.dto.response.IdentityResponse;
import de.ni0.chronoscope.exception.ApiNotImplementedException;
import de.ni0.chronoscope.mapper.IdentityMapper;
import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.IdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Identity", description = "Retrieve the current identity and manage account linking")
@RestController
@RequestMapping("/v1/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;
    private final AccountService accountService;
    private final RequestContext requestContext;
    private final IdentityMapper identityMapper;

    @Operation(summary = "Get current identity", description = "Retrieve information about the identity contained in the access token, including all linked accounts.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Identity retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public IdentityResponse getIdentity() {
        long identityId = this.requestContext.getIdentityId();
        return this.identityMapper.toResponse(this.identityService.getIdentity(identityId));
    }

    @Operation(summary = "Request account linking", description = "Send a confirmation link to the target e-mail address. If the target accepts, the two accounts' identities are merged.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Link e-mail sent"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestAccountLink(@Valid @RequestBody AccountLinkRequest request) {
        throw new ApiNotImplementedException();
    }

    @Operation(summary = "Confirm account linking", description = "Confirm a pending account link using the token received via e-mail. Returns the IDs of both linked accounts.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account link confirmed"),
        @ApiResponse(responseCode = "400", description = "Validation error or invalid/expired token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/accounts/confirm")
    public AccountLinkConfirmResponse confirmAccountLink(@Valid @RequestBody AccountLinkConfirmRequest request) {
        throw new ApiNotImplementedException();
    }
}
