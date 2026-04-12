package de.ni0.chronoscope.service;

import de.ni0.chronoscope.exception.InsufficientSlotsException;
import de.ni0.chronoscope.model.OrganizationSlot;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.repository.OrganizationSlotRepository;
import de.ni0.chronoscope.repository.ScopeRepository;
import de.ni0.chronoscope.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanningService {

    private final OrganizationSlotRepository organizationSlotRepository;
    private final TaskRepository taskRepository;
    private final ScopeRepository scopeRepository;

    @Transactional
    public List<Scope> planScopes() {
        List<Scope> scopes = new ArrayList<>();

        return scopes;
    }
}
