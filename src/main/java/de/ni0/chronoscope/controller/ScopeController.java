package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.response.ScopeResponse;
import de.ni0.chronoscope.exception.ApiNotImplementedException;
import de.ni0.chronoscope.service.ScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/scopes")
@RequiredArgsConstructor
public class ScopeController {

    private final ScopeService scopeService;

    @GetMapping
    public List<ScopeResponse> getScopes() {
        throw new ApiNotImplementedException();
    }
}
