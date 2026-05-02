package de.ni0.chronoscope.config;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import lombok.Getter;
import lombok.Setter;

@Component
@Setter
@Getter
@RequestScope
public class RequestContext {
    private long identityId;
    private long accountId;
    private List<String> adminOrganizations = List.of();

}
