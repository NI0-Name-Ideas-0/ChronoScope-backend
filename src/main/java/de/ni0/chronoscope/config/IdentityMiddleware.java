package de.ni0.chronoscope.config;

import java.io.IOException;

import de.ni0.chronoscope.model.Account;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import de.ni0.chronoscope.service.AccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Per-request filter that synchronizes JWT account data and stores identity context for controllers.
 */
@RequiredArgsConstructor
@Component
public class IdentityMiddleware extends OncePerRequestFilter {

    private final AccountService accountService;
    private final RequestContext requestContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            String subject = jwt.getSubject();
            Account account = this.accountService.syncAccount(subject);
            this.requestContext.setAccount(account);
        }
        filterChain.doFilter(request, response);
    }
}
