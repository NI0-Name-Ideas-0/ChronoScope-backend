package de.ni0.chronoscope.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import de.ni0.chronoscope.TestData;
import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.model.*;
import de.ni0.chronoscope.service.KeycloakService;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import de.ni0.chronoscope.repository.TaskRepository;
import de.ni0.chronoscope.repository.WorkSlotRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = "de.ni0.chronoscope.mapper")
@Transactional
@ExtendWith(MockitoExtension.class)
class PlanControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WorkSlotRepository workSlotRepository;

    @MockitoBean
    private KeycloakService keycloakService;

    private String uniqueSubject() {
        return "it-plan-subject-" + System.nanoTime();
    }

    /** Creates an identity + account without any organizations pre-linked. */
    private Account createAccount() {
        Account account = TestData.account();
        account.setIdentity(identityRepository.saveAndFlush(account.getIdentity()));
        return accountRepository.saveAndFlush(account);
    }

    private long createDynamicTask(Identity identity, String orgId) {
        DynamicTask task = new DynamicTask();
        task.setIdentity(identity);
        task.setOrganizationId(orgId);
        task.setName("it-plan-task-" + System.nanoTime());
        task.setDescription("Task for planning IT test");
        task.setDifficulty(Task.Difficulty.TRIVIAL);
        task.setStartAt(Instant.parse("2026-04-26T06:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-26T20:00:00Z"));
        task.setDuration(Duration.of(60, ChronoUnit.MINUTES));
        task.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        task.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        task.setMaxScopeDuration(Duration.of(60, ChronoUnit.MINUTES));
        task.setLabels(new ArrayList<>());
        task.setScopes(new ArrayList<>());
        task.setDependencies(new HashSet<>());
        task.setDependents(new HashSet<>());
        return taskRepository.saveAndFlush(task).getId();
    }

    private void createWorkSlot(Identity identity, String org, String startAt, String endAt) {
        WorkSlot slot = new WorkSlot();
        slot.setIdentity(identity);
        slot.setOrganizationId(org);
        slot.setStartAt(Instant.parse(startAt));
        slot.setEndAt(Instant.parse(endAt));
        workSlotRepository.saveAndFlush(slot);
    }

    private OrganizationRepresentation organization(String id) {
        OrganizationRepresentation organization = new OrganizationRepresentation();
        organization.setId(id);
        return organization;
    }

    @Test
    void plan_Returns401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/v1/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "accountId": 1, "organizationId": 1 }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void plan_Returns200WithScopesAcrossOrganizations_WhenOrganizationIdMissing() throws Exception {
        Account account = createAccount();
        String orgA = UUID.randomUUID().toString();
        String orgB = UUID.randomUUID().toString();
        createDynamicTask(account.getIdentity(), orgA);
        createDynamicTask(account.getIdentity(), orgB);
        createWorkSlot(account.getIdentity(), orgA, "2026-04-26T06:00:00Z", "2026-04-26T08:00:00Z");
        createWorkSlot(account.getIdentity(), orgB, "2026-04-26T10:00:00Z", "2026-04-26T12:00:00Z");

        when(keycloakService.getIdentityOrganizations(any(Identity.class)))
                .thenReturn(Set.of(organization(orgA), organization(orgB)));

        mockMvc.perform(post("/v1/plan")
                        .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].startAt", hasItem("2026-04-26T06:00:00Z")))
                .andExpect(jsonPath("$[*].startAt", hasItem("2026-04-26T10:00:00Z")));
    }

    @Test
    void plan_Returns403_WhenNoAccessToOrganization() throws Exception {
        String subject = uniqueSubject();

        doThrow(AccountAccessDeniedException.class).when(keycloakService)
                .validateIdentityOrgAccess(any(), any());
        mockMvc.perform(post("/v1/plan")
                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "organizationId": 1 }
                                """))
                .andExpect(status().is(403));
    }

    @Test
    void plan_Returns200WithEmptyList_WhenNoTasksForOrganization() throws Exception {
        Account account = createAccount();
        String orgId = UUID.randomUUID().toString();

        String payload = """
                { "organizationId": "%s" }
                """.formatted(orgId);

        mockMvc.perform(post("/v1/plan")
                        .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void plan_Returns409_WhenInsufficientWorkSlots() throws Exception {
        Account account = createAccount();
        String orgId = UUID.randomUUID().toString();
        createDynamicTask(account.getIdentity(), orgId);
        // No work slots created for this identity → algorithm throws InsufficientSlotsException

        String payload = """
                { "organizationId": "%s" }
                """.formatted(orgId);

        mockMvc.perform(post("/v1/plan")
                        .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void plan_Returns200WithScopes_WhenPlanSucceeds() throws Exception {
        Account account = createAccount();
        String orgId = UUID.randomUUID().toString();
        createDynamicTask(account.getIdentity(), orgId);
        createWorkSlot(account.getIdentity(), orgId, "2026-04-26T06:00:00Z", "2026-04-26T20:00:00Z");

        String payload = """
                { "organizationId": "%s" }
                """.formatted(orgId);

        mockMvc.perform(post("/v1/plan")
                        .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].startAt").value("2026-04-26T06:00:00Z"))
                .andExpect(jsonPath("$[0].endAt").value("2026-04-26T07:00:00Z"));
    }
}
