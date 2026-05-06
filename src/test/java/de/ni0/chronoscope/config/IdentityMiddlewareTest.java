package de.ni0.chronoscope.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

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
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class IdentityMiddlewareTest {

    @Mock
    private AccountService accountService;

    @Mock
    private FilterChain filterChain;

    @Test
    void doFilterInternal_SyncsAccountAndStoresItInRequestContext() throws ServletException, IOException {
        RequestContext requestContext = new RequestContext();
        IdentityMiddleware middleware = new IdentityMiddleware(accountService, requestContext);

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "subject-123")
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

            middleware.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

            assertEquals(11L, requestContext.getAccount().getId());
            assertEquals(22L, requestContext.getAccount().getIdentity().getId());
            verify(accountService).syncAccount(eq("subject-123"));
            verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
            verifyNoMoreInteractions(accountService, filterChain);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
