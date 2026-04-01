package de.ni0.chronoscope.controller.dto;

import de.ni0.chronoscope.model.Scope;
import lombok.Data;

import java.util.List;

@Data
public class PlanScopesResponse {
    private List<Scope> scopes;

    public PlanScopesResponse(List<Scope> scopes) {
        this.scopes = scopes;
    }
}
