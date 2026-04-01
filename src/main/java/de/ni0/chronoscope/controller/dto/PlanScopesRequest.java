package de.ni0.chronoscope.controller.dto;

import de.ni0.chronoscope.model.OrganizationSlot;
import de.ni0.chronoscope.model.Task;
import lombok.Data;

import java.util.List;

@Data
public class PlanScopesRequest {
    private List<Task> tasks;
    private List<OrganizationSlot> slots;
}
