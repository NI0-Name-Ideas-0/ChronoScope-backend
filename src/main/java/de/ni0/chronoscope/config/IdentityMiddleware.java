package de.ni0.chronoscope.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.IdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class IdentityMiddleware extends OncePerRequestFilter {
    private final AccountService accountService;
    private final IdentityService identityService;
    private final RequestContext requestContext;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            List<String> organizations = jwt.getClaimAsStringList("organization");
            if (organizations == null || organizations.isEmpty()) {
                organizations = List.of("private");
            } else {
                organizations = List.copyOf(organizations);
            }
            String subject = jwt.getSubject();
            long accountId = this.accountService.syncAccount(subject, organizations);
            long identityId = this.identityService.syncIdentity(subject);
            this.requestContext.setIdentityId(identityId);
            this.requestContext.setAccountId(accountId);
        }
        filterChain.doFilter(request, response);
    }
}
