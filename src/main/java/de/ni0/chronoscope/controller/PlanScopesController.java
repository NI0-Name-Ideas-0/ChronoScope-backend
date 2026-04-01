package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.dto.PlanScopesRequest;
import de.ni0.chronoscope.dto.PlanScopesResponse;
import de.ni0.chronoscope.exception.InsufficientSlotsException;
import de.ni0.chronoscope.model.OrganizationSlot;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.service.PlanScopesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1/planScopes")
public class PlanScopesController {

    private final PlanScopesService planScopesService;

    public PlanScopesController(PlanScopesService planScopesService) {
        this.planScopesService = planScopesService;
    }

    @GetMapping("/test")
    public PlanScopesResponse testPlan() {
        Task task = new Task();
        task.setName("test");
        task.setComplexity(1);
        task.setDuration(Duration.ofHours(10));
        task.setOrganizationId(1);

        OrganizationSlot slot1 = new OrganizationSlot();
        slot1.setStart(Instant.parse("2026-03-26T06:30:00Z"));
        slot1.setDuration(Duration.ofHours(5));

        OrganizationSlot slot2 = new OrganizationSlot();
        slot2.setStart(Instant.parse("2026-03-26T14:30:00Z"));
        slot2.setDuration(Duration.ofHours(5));

        List<Scope> scopes = planScopesService.planScopes(List.of(task), List.of(slot1, slot2));
        return new PlanScopesResponse(scopes);
    }

    @PostMapping
    public ResponseEntity<PlanScopesResponse> plan(@RequestBody PlanScopesRequest request) {
        List<Scope> scopes = planScopesService.planScopes(request.getTasks(), request.getSlots());
        return ResponseEntity.ok(new PlanScopesResponse(scopes));
    }

    @ExceptionHandler(InsufficientSlotsException.class)
    public ResponseEntity<String> handleInsufficientSlots(InsufficientSlotsException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
