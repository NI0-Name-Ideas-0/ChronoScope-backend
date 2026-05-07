package de.ni0.chronoscope.config;

import java.util.List;

import de.ni0.chronoscope.model.Account;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import lombok.Getter;
import lombok.Setter;

/**
 * Request-scoped holder for authenticated identity data extracted by {@link IdentityMiddleware}.
 */
@Component
@Setter
@Getter
@RequestScope
public class RequestContext {
    private Account account;
}
