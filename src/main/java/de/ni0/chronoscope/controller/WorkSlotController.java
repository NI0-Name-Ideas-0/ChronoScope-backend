package de.ni0.chronoscope.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.ni0.chronoscope.controller.dto.WorkSlotDto;
import de.ni0.chronoscope.exception.ApiNotImplementedException;
import de.ni0.chronoscope.service.WorkSlotService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/workslots")
@RequiredArgsConstructor
public class WorkSlotController {

    private final WorkSlotService workSlotService;

    @GetMapping
    public List<WorkSlotDto> getWorkSlots() {
        throw new ApiNotImplementedException();
    }

    @PostMapping
    public void createWorkSlot(@RequestBody WorkSlotDto workSlotDto) {
        throw new ApiNotImplementedException();
    }

    @PutMapping("/{id}")
    public void updateWorkSlot(@PathVariable Long id, @RequestBody WorkSlotDto workSlotDto) {
        throw new ApiNotImplementedException();
    }

    @DeleteMapping("/{id}")
    public void deleteWorkSlot(@PathVariable Long id) {
        throw new ApiNotImplementedException();
    }
}
