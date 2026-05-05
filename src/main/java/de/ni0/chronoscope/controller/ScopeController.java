package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.response.ScopeResponse;
import de.ni0.chronoscope.exception.ApiNotImplementedException;
import de.ni0.chronoscope.service.ScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for planned task scopes.
 */
@Tag(name = "Scopes", description = "Read planned scopes for dynamic tasks")
@RestController
@RequestMapping("/v1/scopes")
@RequiredArgsConstructor
public class ScopeController {

    private final ScopeService scopeService;

    /**
     * Lists planned scopes for the current identity.
     *
     * @return scope responses
     */
    @Operation(summary = "List scopes", description = "List all scopes assigned to dynamic tasks belonging to the current identity.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scopes retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public List<ScopeResponse> getScopes() {
        throw new ApiNotImplementedException();
    }
}
