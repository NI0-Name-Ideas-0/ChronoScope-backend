package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.WorkSlotDto;
import de.ni0.chronoscope.controller.dto.request.PlanRequest;
import de.ni0.chronoscope.exception.ApiNotImplementedException;
import de.ni0.chronoscope.service.PlanningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanningService planningService;

    @PostMapping
    public List<WorkSlotDto> plan(@Valid @RequestBody PlanRequest request) {
        throw new ApiNotImplementedException();
    }
}
