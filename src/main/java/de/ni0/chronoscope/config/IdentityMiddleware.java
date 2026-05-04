package de.ni0.chronoscope.config;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private static final String ORG_ADMIN_GROUP_PREFIX = "org-admin/";

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
            String rawEmail = jwt.getClaimAsString("email");
            String email = rawEmail != null ? rawEmail : "";
            String subject = jwt.getSubject();
            long accountId = this.accountService.syncAccount(subject, email, organizations);
            long identityId = this.identityService.syncIdentity(subject);
            this.requestContext.setIdentityId(identityId);
            this.requestContext.setAccountId(accountId);
            this.requestContext.setAdminOrganizations(extractAdminOrganizations(jwt.getClaimAsStringList("groups")));
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts organization names for which the authenticated account is an admin.
     * Admin membership is represented by JWT group paths like /org-admin/{organizationName}.
     */
    static List<String> extractAdminOrganizations(List<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }

        Set<String> adminOrganizations = new LinkedHashSet<>();
        for (String group : groups) {
            if (group == null) {
                continue;
            }

            String normalizedGroup = group.strip();
            while (normalizedGroup.startsWith("/")) {
                normalizedGroup = normalizedGroup.substring(1);
            }

            if (!normalizedGroup.startsWith(ORG_ADMIN_GROUP_PREFIX)) {
                continue;
            }

            String organizationName = normalizedGroup.substring(ORG_ADMIN_GROUP_PREFIX.length());
            int nestedGroupStart = organizationName.indexOf('/');
            if (nestedGroupStart >= 0) {
                organizationName = organizationName.substring(0, nestedGroupStart);
            }
            organizationName = organizationName.strip();

            if (!organizationName.isEmpty()) {
                adminOrganizations.add(organizationName);
            }
        }

        return List.copyOf(adminOrganizations);
    }
}
