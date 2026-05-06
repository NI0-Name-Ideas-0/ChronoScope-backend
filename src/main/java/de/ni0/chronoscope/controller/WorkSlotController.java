package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.WorkSlotCreateRequest;
import de.ni0.chronoscope.controller.dto.request.WorkSlotUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.WorkSlotResponse;
import de.ni0.chronoscope.mapper.WorkSlotMapper;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.KeycloakService;
import de.ni0.chronoscope.service.WorkSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for availability windows used by the planner.
 */
@Tag(name = "Work Slots", description = "Manage available work slots used by the planning algorithm")
@RestController
@RequestMapping("/v1/workslots")
@RequiredArgsConstructor
public class WorkSlotController {

    private final WorkSlotService workSlotService;
    private final WorkSlotMapper workSlotMapper;
    private final RequestContext requestContext;
    private final KeycloakService keycloakService;

    /**
     * Lists all work slots visible to the authenticated identity.
     *
     * @return work-slot responses
     */
    @Operation(summary = "List work slots", description = "Return all work slots belonging to the current identity.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work slots retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public List<WorkSlotResponse> getWorkSlots() {
        return workSlotService.getWorkSlotsForIdentity(requestContext.getAccount().getIdentity().getId()).stream()
                .map(workSlotMapper::toResponse)
                .toList();
    }

    /**
     * Creates a work slot for an account owned by the authenticated identity.
     *
     * @param request work-slot creation payload
     * @return created work slot
     */
    @Operation(summary = "Create work slot", description = "Create a new available work slot for the given account.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Work slot created"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<WorkSlotResponse> createWorkSlot(@Valid @RequestBody WorkSlotCreateRequest request) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityOrgAccess(identity, request.organizationId());
        WorkSlot workSlot = workSlotMapper.fromCreateRequest(request);
        WorkSlotResponse response = workSlotMapper.toResponse(workSlotService.createWorkSlot(workSlot));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Partially updates the time boundaries of a work slot.
     *
     * @param id work slot ID
     * @param request patch payload
     * @return updated work slot
     */
    @Operation(summary = "Update work slot", description = "Update the start/end times of an existing work slot (PATCH semantics).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work slot updated"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Work slot not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}")
    public WorkSlotResponse updateWorkSlot(
            @Parameter(description = "Work slot ID") @PathVariable Long id,
            @Valid @RequestBody WorkSlotUpdateRequest request) {
        WorkSlot updated = workSlotService.updateWorkSlot(requestContext.getAccount().getIdentity().getId(), id, request);
        return workSlotMapper.toResponse(updated);
    }

    /**
     * Deletes a work slot owned by the authenticated identity.
     *
     * @param id work slot ID
     */
    @Operation(summary = "Delete work slot", description = "Delete a work slot.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Work slot deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Work slot not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkSlot(
            @Parameter(description = "Work slot ID") @PathVariable Long id) {
        workSlotService.deleteWorkSlot(requestContext.getAccount().getIdentity().getId(), id);
    }
}
