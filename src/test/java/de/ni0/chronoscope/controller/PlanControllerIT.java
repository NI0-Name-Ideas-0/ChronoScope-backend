package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.Organization;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import de.ni0.chronoscope.repository.OrganizationRepository;
import de.ni0.chronoscope.repository.TaskRepository;
import de.ni0.chronoscope.repository.WorkSlotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = "de.ni0.chronoscope.mapper")
@Transactional
class PlanControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WorkSlotRepository workSlotRepository;

    private String uniqueSubject() {
        return "it-plan-subject-" + System.nanoTime();
    }

    private String uniqueOrgName() {
        return "it-plan-org-" + System.nanoTime();
    }

    /** Creates an identity + account without any organizations pre-linked. */
    private AccountInfo createAccount(String subject) {
        Identity identity = identityRepository.saveAndFlush(new Identity());
        Account account = new Account();
        account.setIdentity(identity);
        account.setSubject(subject);
        account = accountRepository.saveAndFlush(account);
        return new AccountInfo(identity.getId(), account.getId());
    }

    private long createOrganization(String name) {
        Organization org = new Organization();
        org.setName(name);
        return organizationRepository.saveAndFlush(org).getId();
    }

    private long createDynamicTask(long accountId, long orgId) {
        DynamicTask task = new DynamicTask();
        task.setAccount(accountRepository.getReferenceById(accountId));
        task.setOrganization(organizationRepository.getReferenceById(orgId));
        task.setName("it-plan-task-" + System.nanoTime());
        task.setDescription("Task for planning IT test");
        task.setDifficulty(2);
        task.setStartAt(Instant.parse("2026-04-26T06:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-26T20:00:00Z"));
        task.setRrule("FREQ=DAILY");
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

    private void createWorkSlot(long accountId, String startAt, String endAt) {
        WorkSlot slot = new WorkSlot();
        slot.setAccount(accountRepository.getReferenceById(accountId));
        slot.setStartAt(Instant.parse(startAt));
        slot.setEndAt(Instant.parse(endAt));
        workSlotRepository.saveAndFlush(slot);
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
    void plan_Returns400_WhenOrganizationIdMissing() throws Exception {
        String subject = uniqueSubject();
        String orgName = uniqueOrgName();

        mockMvc.perform(post("/v1/plan")
                        .with(jwt().jwt(jwt -> jwt.subject(subject).claim("organization", List.of(orgName))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "accountId": 1 }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void plan_Returns400_WhenAccountIdMissing() throws Exception {
        String subject = uniqueSubject();
        String orgName = uniqueOrgName();

        mockMvc.perform(post("/v1/plan")
                        .with(jwt().jwt(jwt -> jwt.subject(subject).claim("organization", List.of(orgName))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "organizationId": 1 }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void plan_Returns403_WhenAccountDoesNotBelongToIdentity() throws Exception {
        String subjectA = uniqueSubject();
        String subjectB = uniqueSubject();
        String orgName = uniqueOrgName();

        createAccount(subjectA);
        AccountInfo infoB = createAccount(subjectB);
        long orgId = createOrganization(orgName);

        String payload = """
                { "accountId": %d, "organizationId": %d }
                """.formatted(infoB.accountId(), orgId);

        mockMvc.perform(post("/v1/plan")
                        .with(jwt().jwt(jwt -> jwt.subject(subjectA).claim("organization", List.of(orgName))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void plan_Returns200WithEmptyList_WhenNoTasksForOrganization() throws Exception {
        String subject = uniqueSubject();
        String orgName = uniqueOrgName();
        AccountInfo info = createAccount(subject);
        long orgId = createOrganization(orgName);

        String payload = """
                { "accountId": %d, "organizationId": %d }
                """.formatted(info.accountId(), orgId);

        mockMvc.perform(post("/v1/plan")
                        .with(jwt().jwt(jwt -> jwt.subject(subject).claim("organization", List.of(orgName))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void plan_Returns409_WhenInsufficientWorkSlots() throws Exception {
        String subject = uniqueSubject();
        String orgName = uniqueOrgName();
        AccountInfo info = createAccount(subject);
        long orgId = createOrganization(orgName);
        createDynamicTask(info.accountId(), orgId);
        // No work slots created for this identity → algorithm throws InsufficientSlotsException

        String payload = """
                { "accountId": %d, "organizationId": %d }
                """.formatted(info.accountId(), orgId);

        mockMvc.perform(post("/v1/plan")
                        .with(jwt().jwt(jwt -> jwt.subject(subject).claim("organization", List.of(orgName))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void plan_Returns200WithScopes_WhenPlanSucceeds() throws Exception {
        String subject = uniqueSubject();
        String orgName = uniqueOrgName();
        AccountInfo info = createAccount(subject);
        long orgId = createOrganization(orgName);
        createDynamicTask(info.accountId(), orgId);
        createWorkSlot(info.accountId(), "2026-04-26T06:00:00Z", "2026-04-26T20:00:00Z");

        String payload = """
                { "accountId": %d, "organizationId": %d }
                """.formatted(info.accountId(), orgId);

        mockMvc.perform(post("/v1/plan")
                        .with(jwt().jwt(jwt -> jwt.subject(subject).claim("organization", List.of(orgName))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].startAt").value("2026-04-26T06:00:00Z"))
                .andExpect(jsonPath("$[0].endAt").value("2026-04-26T07:00:00Z"));
    }

    private record AccountInfo(long identityId, long accountId) {}
}
