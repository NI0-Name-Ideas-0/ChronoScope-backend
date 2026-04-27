package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.PlanRequest;
import de.ni0.chronoscope.controller.dto.response.ScopeResponse;
import de.ni0.chronoscope.mapper.ScopeMapper;
import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.PlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Planning", description = "Generate optimized task plans from available work slots")
@RestController
@RequestMapping("/v1/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanningService planningService;

    private final ScopeMapper scopeMapper;

    private final AccountService accountService;

    private final RequestContext requestContext;

    @Operation(summary = "Generate plan", description = "Run the planning algorithm for the given account. Returns planned task scopes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plan generated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Not enough work slots to accommodate all tasks", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public List<ScopeResponse> plan(@Valid @RequestBody PlanRequest request) {
        accountService.validateAccountOwnership(requestContext.getIdentityId(), request.accountId());
        var result = planningService.planTasksForAccount(request.accountId(), request.organizationId());
        return result.stream().map(scopeMapper::toResponse).toList();
    }
}
