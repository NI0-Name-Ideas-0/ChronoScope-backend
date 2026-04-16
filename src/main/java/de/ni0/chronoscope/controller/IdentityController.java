package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.request.AccountLinkConfirmRequest;
import de.ni0.chronoscope.controller.dto.request.AccountLinkRequest;
import de.ni0.chronoscope.controller.dto.response.AccountLinkConfirmResponse;
import de.ni0.chronoscope.controller.dto.response.IdentityResponse;
import de.ni0.chronoscope.exception.ApiNotImplementedException;
import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.IdentityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;
    private final AccountService accountService;

    @GetMapping
    public IdentityResponse getIdentity() {
        throw new ApiNotImplementedException();
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestAccountLink(@Valid @RequestBody AccountLinkRequest request) {
        throw new ApiNotImplementedException();
    }

    @PostMapping("/accounts/confirm")
    public AccountLinkConfirmResponse confirmAccountLink(@Valid @RequestBody AccountLinkConfirmRequest request) {
        throw new ApiNotImplementedException();
    }
}
