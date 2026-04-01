package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.PlanScopesRequest;
import de.ni0.chronoscope.controller.dto.PlanScopesResponse;
import de.ni0.chronoscope.exception.InsufficientSlotsException;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.service.PlanningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/plan")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @PostMapping("/")
    public PlanScopesResponse plan() {
        List<Scope> scopes = planningService.planScopes();
        return new PlanScopesResponse(scopes);
    }

    @PostMapping
    public ResponseEntity<PlanScopesResponse> plan(@RequestBody PlanScopesRequest request) {
        List<Scope> scopes = planningService.planScopes();
        return ResponseEntity.ok(new PlanScopesResponse(scopes));
    }

    @ExceptionHandler(InsufficientSlotsException.class)
    public ResponseEntity<String> handleInsufficientSlots(InsufficientSlotsException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
