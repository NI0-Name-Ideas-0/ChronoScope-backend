package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.request.WorkSlotCreateRequest;
import de.ni0.chronoscope.controller.dto.request.WorkSlotUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.WorkSlotResponse;
import de.ni0.chronoscope.exception.ApiNotImplementedException;
import de.ni0.chronoscope.service.WorkSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/workslots")
@RequiredArgsConstructor
public class WorkSlotController {

    private final WorkSlotService workSlotService;

    @GetMapping
    public List<WorkSlotResponse> getWorkSlots() {
        throw new ApiNotImplementedException();
    }

    @PostMapping
    public ResponseEntity<WorkSlotResponse> createWorkSlot(@Valid @RequestBody WorkSlotCreateRequest request) {
        throw new ApiNotImplementedException();
    }

    @PatchMapping("/{id}")
    public WorkSlotResponse updateWorkSlot(@PathVariable Long id, @Valid @RequestBody WorkSlotUpdateRequest request) {
        throw new ApiNotImplementedException();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkSlot(@PathVariable Long id) {
        throw new ApiNotImplementedException();
    }
}
