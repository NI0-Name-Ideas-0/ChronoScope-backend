package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.request.PlanRequest;
import de.ni0.chronoscope.controller.dto.response.WorkSlotResponse;
import de.ni0.chronoscope.exception.ApiNotImplementedException;
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

    @Operation(summary = "Generate plan", description = "Run the planning algorithm for the given account. Returns the updated work slots with planned task assignments.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plan generated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Not enough work slots to accommodate all tasks", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public List<WorkSlotResponse> plan(@Valid @RequestBody PlanRequest request) {
        throw new ApiNotImplementedException();
    }
}
