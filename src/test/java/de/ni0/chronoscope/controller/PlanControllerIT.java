package de.ni0.chronoscope.controller;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
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

    private static final ZoneId PLANNING_ZONE = ZoneId.of("Europe/Berlin");

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
        LocalDate planningDate = nextPlanningDate(DayOfWeek.SUNDAY);
        return createDynamicTask(identity, orgId,
                instantAt(planningDate, LocalTime.of(8, 0)),
                instantAt(planningDate, LocalTime.of(22, 0)));
    }

    private long createDynamicTask(Identity identity, String orgId, Instant startAt, Instant endAt) {
        DynamicTask task = new DynamicTask();
        task.setIdentity(identity);
        task.setOrganizationId(orgId);
        task.setName("it-plan-task-" + System.nanoTime());
        task.setDescription("Task for planning IT test");
        task.setDifficulty(Task.Difficulty.TRIVIAL);
        task.setStartAt(startAt);
        task.setEndAt(endAt);
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

    private LocalDate nextPlanningDate(DayOfWeek dayOfWeek) {
        return LocalDate.now(PLANNING_ZONE).with(TemporalAdjusters.next(dayOfWeek));
    }

    private Instant instantAt(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(PLANNING_ZONE).toInstant();
    }

    private void createWorkSlot(Identity identity, String org, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        WorkSlot slot = new WorkSlot();
        slot.setIdentity(identity);
        slot.setOrganizationId(org);
        slot.setDayOfWeek(dayOfWeek);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
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
        LocalDate planningDate = nextPlanningDate(DayOfWeek.SUNDAY);
        createDynamicTask(account.getIdentity(), orgA,
            instantAt(planningDate, LocalTime.of(8, 0)),
            instantAt(planningDate, LocalTime.of(22, 0)));
        createDynamicTask(account.getIdentity(), orgB,
            instantAt(planningDate, LocalTime.of(12, 0)),
            instantAt(planningDate, LocalTime.of(22, 0)));
        createWorkSlot(account.getIdentity(), orgA, DayOfWeek.SUNDAY, LocalTime.of(8, 0), LocalTime.of(10, 0));
        createWorkSlot(account.getIdentity(), orgB, DayOfWeek.SUNDAY, LocalTime.of(12, 0), LocalTime.of(14, 0));

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
                .andExpect(jsonPath("$[*].startAt", hasItem(instantAt(planningDate, LocalTime.of(8, 0)).toString())))
                .andExpect(jsonPath("$[*].startAt", hasItem(instantAt(planningDate, LocalTime.of(12, 0)).toString())));
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
        LocalDate planningDate = nextPlanningDate(DayOfWeek.SUNDAY);
        createDynamicTask(account.getIdentity(), orgId);
        createWorkSlot(account.getIdentity(), orgId, DayOfWeek.SUNDAY, LocalTime.of(8, 0), LocalTime.of(22, 0));

        String payload = """
                { "organizationId": "%s" }
                """.formatted(orgId);

        mockMvc.perform(post("/v1/plan")
                        .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].startAt").value(instantAt(planningDate, LocalTime.of(8, 0)).toString()))
                .andExpect(jsonPath("$[0].endAt").value(instantAt(planningDate, LocalTime.of(9, 0)).toString()));
    }
}
