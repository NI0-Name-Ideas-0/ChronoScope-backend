package de.ni0.chronoscope.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

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
        IdentityMiddleware middleware = new IdentityMiddleware(accountService, identityService, requestContext);

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "subject-123")
            .claim("organization", List.of())
            .build();
        Authentication authentication = new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(accountService.syncAccount("subject-123", List.of("private"))).thenReturn(11L);
        when(identityService.syncIdentity("subject-123")).thenReturn(22L);

        middleware.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

        assertEquals(11L, requestContext.getAccountId());
        assertEquals(22L, requestContext.getIdentityId());
        verify(accountService).syncAccount(eq("subject-123"), eq(List.of("private")));
        verify(identityService).syncIdentity("subject-123");
        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verifyNoMoreInteractions(accountService, identityService, filterChain);
        SecurityContextHolder.clearContext();
    }
}