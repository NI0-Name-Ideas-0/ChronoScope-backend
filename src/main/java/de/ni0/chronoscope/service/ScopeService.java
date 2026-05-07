package de.ni0.chronoscope.service;

import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.repository.ScopeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service placeholder for future planned-scope read operations.
 */
@Service
@RequiredArgsConstructor
public class ScopeService {
    private final ScopeRepository scopeRepository;

    public Set<Scope> getScopes(Identity identity) {
        return this.scopeRepository.getScopesByDynamicTaskIdentityId(identity.getId());
    }

}
