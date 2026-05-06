package de.ni0.chronoscope.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.IdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class IdentityMiddlewareTest {

    @Mock
    private AccountService accountService;

    @Mock
    private IdentityService identityService;

    @Mock
    private FilterChain filterChain;

    @Test
    void doFilterInternal_UsesPrivateOrganizationWhenClaimIsEmpty() throws ServletException, IOException {
        RequestContext requestContext = new RequestContext();
        IdentityMiddleware middleware = new IdentityMiddleware(accountService, requestContext);

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "subject-123")
            .claim("email", "")
            .claim("organizationId", List.of())
            .claim("groups", List.of("/org-admin/dhbw-stuttgart", "/other-group", "/org-admin/dhbw-stuttgart"))
            .build();
        Authentication authentication = new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            Identity identity = new Identity();
            identity.setId(22L);
            Account account = new Account();
            account.setId(11L);
            account.setIdentity(identity);
            when(accountService.syncAccount("subject-123")).thenReturn(account);
            when(identityService.syncIdentity("subject-123")).thenReturn(identity.getId());

            middleware.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

            assertEquals(11L, requestContext.getAccount().getId());
            assertEquals(22L, requestContext.getAccount().getIdentity().getId());
            verify(accountService).syncAccount(eq("subject-123"));
            verify(identityService).syncIdentity(eq("subject-123"));
            verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
            verifyNoMoreInteractions(accountService, identityService, filterChain);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void extractAdminOrganizations_ReturnsNamesAfterOrgAdminGroup() {
        List<String> result = IdentityMiddleware.extractAdminOrganizations(List.of(
            "/org-admin/dhbw-stuttgart",
            "org-admin/chronoscope-local/nested",
            "/users/dhbw-stuttgart",
            "/org-admin/",
            " /org-admin/dhbw-stuttgart "
        ));

        assertEquals(List.of("dhbw-stuttgart", "chronoscope-local"), result);
    }
}
