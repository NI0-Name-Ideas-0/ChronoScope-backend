package de.ni0.chronoscope.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import de.ni0.chronoscope.repository.ScopeRepository;
import de.ni0.chronoscope.repository.TaskRepository;
import de.ni0.chronoscope.service.KeycloakService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskControllerIT {

    private static final String DEFAULT_ORGANIZATION_ID = "test-organization";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @MockitoBean
    private KeycloakService keycloakService;

    @PersistenceContext
    private EntityManager entityManager;

    private Account createAccount() {
        return createAccount(new Identity(), UUID.randomUUID().toString());
    }

    private Account createAccount(Identity identity) {
        return createAccount(identity, UUID.randomUUID().toString());
    }

    private Account createAccount(Identity identity, String subject) {
        Identity managedIdentity = identityRepository.saveAndFlush(identity);
        Account account = new Account();
        account.setSubject(subject);
        account.setIdentity(managedIdentity);
        Account savedAccount = accountRepository.saveAndFlush(account);
        managedIdentity.getAccounts().add(savedAccount);
        return savedAccount;
    }

    private RequestPostProcessor createJwt(String subject) {
        return jwt().jwt(jwt -> jwt
            .subject(subject));
    }

    private long createDynamicPredecessorTask(Identity identity) {
        DynamicTask predecessor = new DynamicTask();
        predecessor.setIdentity(identity);
        predecessor.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        predecessor.setName("Predecessor task");
        predecessor.setDescription("Predecessor task for dependency test");
        predecessor.setDifficulty(2);
        predecessor.setStartAt(Instant.parse("2026-04-20T08:00:00Z"));
        predecessor.setEndAt(Instant.parse("2026-04-22T18:00:00Z"));
        predecessor.setDuration(Duration.of(120, ChronoUnit.MINUTES));
        predecessor.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        predecessor.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        predecessor.setMaxScopeDuration(Duration.of(60, ChronoUnit.MINUTES));
        predecessor.setLabels(new ArrayList<>());
        predecessor.setScopes(new ArrayList<>());
        predecessor.setDependencies(new HashSet<>());
        predecessor.setDependents(new HashSet<>());
        return this.taskRepository.saveAndFlush(predecessor).getId();
    }

    @Test
    void getTasks_ReturnsTasksFromCurrentIdentityAndLinkedAccountsOnly() throws Exception {
        String subject = "it-get-tasks-subject-" + System.nanoTime();
        Identity identity = identityRepository.saveAndFlush(new Identity());
        createAccount(identity, subject);

        Identity otherIdentity = identityRepository.saveAndFlush(new Identity());
        createAccount(otherIdentity);

        String dynamicTaskName = "it-dynamic-task-" + System.nanoTime();
        String linkedStaticTaskName = "it-linked-static-task-" + System.nanoTime();
        String foreignTaskName = "it-foreign-task-" + System.nanoTime();

        DynamicTask ownTask = new DynamicTask();
        ownTask.setIdentity(identity);
        ownTask.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        ownTask.setName(dynamicTaskName);
        ownTask.setDescription("Dynamic task in current identity");
        ownTask.setDifficulty(2);
        ownTask.setStartAt(Instant.parse("2026-04-20T08:00:00Z"));
        ownTask.setEndAt(Instant.parse("2026-04-22T18:00:00Z"));
        ownTask.setDuration(Duration.of(120, ChronoUnit.MINUTES));
        ownTask.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        ownTask.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        ownTask.setMaxScopeDuration(Duration.of(60, ChronoUnit.MINUTES));
        ownTask.setLabels(new ArrayList<>());
        ownTask.setScopes(new ArrayList<>());
        ownTask.setDependencies(new HashSet<>());
        ownTask.setDependents(new HashSet<>());
        this.taskRepository.saveAndFlush(ownTask);

        StaticTask linkedTask = new StaticTask();
        linkedTask.setIdentity(identity);
        linkedTask.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        linkedTask.setName(linkedStaticTaskName);
        linkedTask.setDescription("Static task in linked account");
        linkedTask.setDifficulty(1);
        linkedTask.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        linkedTask.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        linkedTask.setRrule("FREQ=DAILY");
        linkedTask.setLabels(new ArrayList<>());
        linkedTask.setIsBlocker(false);
        this.taskRepository.saveAndFlush(linkedTask);

        StaticTask foreignTask = new StaticTask();
        foreignTask.setIdentity(otherIdentity);
        foreignTask.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        foreignTask.setName(foreignTaskName);
        foreignTask.setDescription("Task from another identity");
        foreignTask.setDifficulty(1);
        foreignTask.setStartAt(Instant.parse("2026-04-20T11:00:00Z"));
        foreignTask.setEndAt(Instant.parse("2026-04-20T12:00:00Z"));
        foreignTask.setRrule("FREQ=DAILY");
        foreignTask.setLabels(new ArrayList<>());
        foreignTask.setIsBlocker(false);
        this.taskRepository.saveAndFlush(foreignTask);

        mockMvc.perform(get("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt
                    .subject(subject)
                    .claim("organizationId", List.of("private")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", hasItem(dynamicTaskName)))
            .andExpect(jsonPath("$[*].name", hasItem(linkedStaticTaskName)))
            .andExpect(jsonPath("$[*].name", not(hasItem(foreignTaskName))))
            .andExpect(jsonPath("$[*].type", hasItem("dynamic")))
            .andExpect(jsonPath("$[*].type", hasItem("static")));
    }

    @Test
    void createTask_Static_ReturnsCreated() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();

        String payload = """
            {
              "type": "static",
              "organizationId": "%s",
              "name": "Write report",
              "description": "Prepare weekly summary",
              "rrule": "FREQ=WEEKLY;BYDAY=MO",
              "difficulty": 3,
              "startAt": "2026-04-20T09:00:00Z",
              "endAt": "2026-04-20T10:00:00Z",
              "labels": [],
              "isBlocker": false
            }
            """.formatted(organizationId);

        mockMvc.perform(post("/v1/tasks")
                .with(createJwt(account.getSubject()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.organizationId").value(organizationId))
            .andExpect(jsonPath("$.name").value("Write report"))
            .andExpect(jsonPath("$.description").value("Prepare weekly summary"))
            .andExpect(jsonPath("$.difficulty").value(3))
            .andExpect(jsonPath("$.rrule").value("FREQ=WEEKLY;BYDAY=MO"))
            .andExpect(jsonPath("$.isBlocker").value(false));
    }

    @Test
    void createTask_Static_BlockerWithoutOrganization_ReturnsCreated() throws Exception {
        Account account = createAccount();

        String payload = """
            {
              "type": "static",
              "name": "Maintenance window",
              "description": "Block planning for this time",
              "rrule": "FREQ=DAILY;COUNT=1",
              "difficulty": 1,
              "startAt": "2026-04-20T09:00:00Z",
              "endAt": "2026-04-20T10:00:00Z",
              "labels": [],
              "isBlocker": true
            }
            """;

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.organizationId").value(nullValue()))
            .andExpect(jsonPath("$.name").value("Maintenance window"))
            .andExpect(jsonPath("$.isBlocker").value(true));
    }

    @Test
    void createTask_Static_NonBlockerWithoutOrganization_ReturnsValidationError() throws Exception {
        Account account = createAccount();

        String payload = """
            {
              "type": "static",
              "name": "Write report",
              "description": "Prepare weekly summary",
              "rrule": "FREQ=WEEKLY;BYDAY=MO",
              "difficulty": 3,
              "startAt": "2026-04-20T09:00:00Z",
              "endAt": "2026-04-20T10:00:00Z",
              "labels": [],
              "isBlocker": false
            }
            """;

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("organizationId")));
    }

    @Test
    void createTask_Static_DifficultyAboveFive_ReturnsValidationError() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();

        String payload = """
            {
              "type": "static",
              "organizationId": "%s",
              "name": "Write report",
              "description": "Prepare weekly summary",
              "rrule": "FREQ=WEEKLY;BYDAY=MO",
              "difficulty": 6,
              "startAt": "2026-04-20T09:00:00Z",
              "endAt": "2026-04-20T10:00:00Z",
              "labels": [],
              "isBlocker": false
            }
            """.formatted(organizationId);

        mockMvc.perform(post("/v1/tasks")
            .with(createJwt(account.getSubject()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("difficulty")));
    }

    @Test
    void createTask_Dynamic_ReturnsCreatedWithDefaults() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();

        String payload = """
            {
              "type": "dynamic",
              "organizationId": "%s",
              "name": "Implement API endpoint",
              "description": "Create and test endpoint",
              "difficulty": 4,
              "startAt": "2026-04-20T08:00:00Z",
              "endAt": "2026-04-25T18:00:00Z",
              "labels": [],
              "duration": "PT240M",
              "minScopeDuration": "PT30M",
                            "maxScopeDuration": "PT90M",
              "dependencies": []
            }
            """.formatted(organizationId);

        mockMvc.perform(post("/v1/tasks")
                .with(createJwt(account.getSubject()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.organizationId").value(organizationId))
            .andExpect(jsonPath("$.name").value("Implement API endpoint"))
            .andExpect(jsonPath("$.difficulty").value(4))
            .andExpect(jsonPath("$.duration").value("PT4H"))
            .andExpect(jsonPath("$.elapsed").value("PT0S"))
            .andExpect(jsonPath("$.minScopeDuration").value("PT30M"))
            .andExpect(jsonPath("$.maxScopeDuration").value("PT1H30M"))
            .andExpect(jsonPath("$.scopes").isArray())
            .andExpect(jsonPath("$.scopes").isEmpty())
            .andExpect(jsonPath("$.dependencies").isArray())
            .andExpect(jsonPath("$.dependencies").isEmpty())
            .andExpect(jsonPath("$.dependents").isArray())
            .andExpect(jsonPath("$.dependents").isEmpty());
    }

    @Test
    void createTask_Static_WhenKeycloakDeniesOrganization_ReturnsForbidden() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();
        doThrow(new AccountAccessDeniedException("Account does not have access to the specified organizationId"))
            .when(keycloakService).validateIdentityOrgAccess(any(Identity.class), eq(organizationId));

        String payload = """
            {
              "type": "static",
              "organizationId": "%s",
              "name": "Write report",
              "description": "Prepare weekly summary",
              "rrule": "FREQ=WEEKLY;BYDAY=MO",
              "difficulty": 3,
              "startAt": "2026-04-20T09:00:00Z",
              "endAt": "2026-04-20T10:00:00Z",
              "labels": [],
              "isBlocker": false
            }
            """.formatted(organizationId);

        mockMvc.perform(post("/v1/tasks")
                .with(createJwt(account.getSubject()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:access-denied"))
            .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.detail").value("Account does not have access to the specified organizationId"));
    }

    @Test
    void createTask_Static_WithOrganizationOutsideAccount_ReturnsForbidden() throws Exception {
        Account account = createAccount();
        String inaccessibleOrganizationId = UUID.randomUUID().toString();
        doThrow(new AccountAccessDeniedException("Account does not have access to the specified organizationId"))
            .when(keycloakService).validateIdentityOrgAccess(any(Identity.class), eq(inaccessibleOrganizationId));

        String payload = """
            {
              "type": "static",
              "organizationId": "%s",
              "name": "Write report",
              "description": "Prepare weekly summary",
              "rrule": "FREQ=WEEKLY;BYDAY=MO",
              "difficulty": 3,
              "startAt": "2026-04-20T09:00:00Z",
              "endAt": "2026-04-20T10:00:00Z",
              "labels": [],
              "isBlocker": false
            }
            """.formatted(inaccessibleOrganizationId);

        mockMvc.perform(post("/v1/tasks")
                .with(createJwt(account.getSubject()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:access-denied"))
            .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void createTask_Dynamic_WithStartAtAfterEndAt_ReturnsValidationError() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();

        String payload = """
            {
              "type": "dynamic",
              "organizationId": "%s",
              "name": "Implement API endpoint",
              "description": "Create and test endpoint",
              "difficulty": 4,
              "startAt": "2026-04-25T18:00:00Z",
              "endAt": "2026-04-20T08:00:00Z",
              "labels": [],
              "duration": "PT240M",
              "minScopeDuration": "PT30M",
                            "maxScopeDuration": "PT90M",
              "dependencies": []
            }
            """.formatted(organizationId);

        mockMvc.perform(post("/v1/tasks")
            .with(createJwt(account.getSubject()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.detail").value("startAt must be before endAt"));
    }

    @Test
    void createTask_Dynamic_InvalidDurationFormat_ReturnsInvalidRequest() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();
        
        String payload = """
            {
              "type": "dynamic",
              "organizationId": "%s",
              "name": "Implement API endpoint",
              "description": "Create and test endpoint",
              "difficulty": 4,
              "startAt": "2026-04-20T08:00:00Z",
              "endAt": "2026-04-25T18:00:00Z",
              "labels": [],
              "duration": "not-a-duration",
              "minScopeDuration": "PT30M",
              "maxScopeDuration": "PT90M",
              "dependencies": []
            }
            """.formatted(organizationId);

        mockMvc.perform(post("/v1/tasks")
            .with(createJwt(account.getSubject()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:invalid-request"))
            .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.detail").value("Request body could not be parsed"));
    }

    @Test
    void createTask_WithMalformedJson_ReturnsInvalidRequest() throws Exception {
        Account account = createAccount();

        String payload = """
            {
              "type": "static",
              "name": "Write report"
            """;

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject()).claim("email", "")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:invalid-request"))
            .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.detail").value("Request body could not be parsed"));
    }

    @Test
    void createTask_Dynamic_WithDependencies_ReturnsCreatedWithDependencyLinks() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();
        long predecessorId = createDynamicPredecessorTask(account.getIdentity());

        String payload = """
            {
              "type": "dynamic",
              "organizationId": "%s",
              "name": "Task with dependencies",
              "description": "Should link predecessor",
              "difficulty": 4,
              "startAt": "2026-04-20T08:00:00Z",
              "endAt": "2026-04-25T18:00:00Z",
              "labels": [],
              "duration": "PT240M",
              "minScopeDuration": "PT30M",
              "maxScopeDuration": "PT90M",
              "dependencies": [
                  %d
              ]
            }
            """.formatted(organizationId, predecessorId);

        mockMvc.perform(post("/v1/tasks")
                .with(createJwt(account.getSubject()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.dependencies").isArray())
            .andExpect(jsonPath("$.dependencies.length()").value(1))
            .andExpect(jsonPath("$.dependencies[0]").value(predecessorId))
            .andExpect(jsonPath("$.dependents").isArray())
            .andExpect(jsonPath("$.dependents").isEmpty());
    }

        @Test
        void createTask_Dynamic_WithDependencyFromSameIdentity_ReturnsCreated() throws Exception {
            Account account = createAccount();
            String organizationId = UUID.randomUUID().toString();
            long predecessorId = createDynamicPredecessorTask(account.getIdentity());

            String payload = """
                    {
                        "type": "dynamic",
                        "organizationId": "%s",
                        "name": "Cross-account dependency",
                        "description": "Dependency should be allowed within same identity",
                        "difficulty": 4,
                        "startAt": "2026-04-20T08:00:00Z",
                        "endAt": "2026-04-25T18:00:00Z",
                        "labels": [],
                        "duration": "PT240M",
                        "minScopeDuration": "PT30M",
                        "maxScopeDuration": "PT90M",
                        "dependencies": [
                            %d
                        ]
                    }
                    """.formatted(organizationId, predecessorId);

            mockMvc.perform(post("/v1/tasks")
                            .with(createJwt(account.getSubject()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.organizationId").value(organizationId))
                    .andExpect(jsonPath("$.dependencies").isArray())
                    .andExpect(jsonPath("$.dependencies.length()").value(1))
                    .andExpect(jsonPath("$.dependencies[0]").value(predecessorId));
        }

        @Test
        void createTask_Dynamic_WithDependencyFromDifferentIdentity_ReturnsValidationError() throws Exception {
            String organizationId = UUID.randomUUID().toString();
            Identity identity = new Identity();
            Identity otherIdentity = new Identity();
            Account account = this.createAccount(identity);
            Account otherAccount = this.createAccount(otherIdentity);

            long predecessorId = createDynamicPredecessorTask(otherAccount.getIdentity());

            String payload = """
                    {
                        "type": "dynamic",
                        "organizationId": "%s",
                        "name": "Cross-identity dependency",
                        "description": "Dependency should be rejected",
                        "difficulty": 4,
                        "startAt": "2026-04-20T08:00:00Z",
                        "endAt": "2026-04-25T18:00:00Z",
                        "labels": [],
                        "duration": "PT240M",
                        "minScopeDuration": "PT30M",
                        "maxScopeDuration": "PT90M",
                        "dependencies": [
                            %d
                        ]
                    }
                    """.formatted(organizationId, predecessorId);

            mockMvc.perform(post("/v1/tasks")
                            .with(createJwt(account.getSubject()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.detail").value("Dependency task " + predecessorId + " must belong to the same identity"));
        }

        @Test
        void getTask_Dynamic_ReturnsDependenciesAndDependentsFields() throws Exception {
            Account account = createAccount();
            long predecessorId = createDynamicPredecessorTask(account.getIdentity());

            DynamicTask dependent = new DynamicTask();
            dependent.setIdentity(account.getIdentity());
            dependent.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            dependent.setName("Dependent dynamic task");
            dependent.setDescription("Depends on predecessor");
            dependent.setDifficulty(3);
            dependent.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
            dependent.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
            dependent.setDuration(Duration.of(180, ChronoUnit.MINUTES));
            dependent.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
            dependent.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
            dependent.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
            dependent.setLabels(new ArrayList<>());
            dependent.setScopes(new ArrayList<>());
            dependent.setDependencies(new HashSet<>(List.of((DynamicTask) taskRepository.getReferenceById(predecessorId))));
            dependent.setDependents(new HashSet<>());
            dependent = this.taskRepository.saveAndFlush(dependent);

            mockMvc.perform(get("/v1/tasks/{id}", dependent.getId())
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dependent.getId()))
                .andExpect(jsonPath("$.type").value("dynamic"))
                .andExpect(jsonPath("$.dependencies").isArray())
                .andExpect(jsonPath("$.dependencies.length()").value(1))
                .andExpect(jsonPath("$.dependencies[0]").value(predecessorId))
                .andExpect(jsonPath("$.dependents").isArray())
                .andExpect(jsonPath("$.dependents").isEmpty());
            }

    @Test
    void updateTask_Static_PartiallyUpdatesOnlyProvidedFields() throws Exception {
        Account account = createAccount();

        StaticTask task = new StaticTask();
        task.setIdentity(account.getIdentity());
        task.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        task.setName("Original static task");
        task.setDescription("Original description");
        task.setDifficulty(2);
        task.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setLabels(new ArrayList<>());
        task.setIsBlocker(false);
        long taskId = this.taskRepository.saveAndFlush(task).getId();

        String payload = """
            {
              "type": "static",
              "name": "Updated static task"
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(taskId))
            .andExpect(jsonPath("$.type").value("static"))
            .andExpect(jsonPath("$.name").value("Updated static task"))
            .andExpect(jsonPath("$.description").value("Original description"))
            .andExpect(jsonPath("$.difficulty").value(2))
            .andExpect(jsonPath("$.rrule").value("FREQ=DAILY"))
            .andExpect(jsonPath("$.isBlocker").value(false));
    }

    @Test
    void updateTask_Static_WithOrganizationOutsideAccount_ReturnsForbidden() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();
        String inaccessibleOrganizationId = UUID.randomUUID().toString();
        doThrow(new AccountAccessDeniedException("Account does not have access to the specified organizationId"))
            .when(keycloakService).validateIdentityOrgAccess(any(Identity.class), eq(inaccessibleOrganizationId));

        StaticTask task = new StaticTask();
        task.setIdentity(account.getIdentity());
        task.setOrganizationId(organizationId);
        task.setName("Original static task");
        task.setDescription("Original description");
        task.setDifficulty(2);
        task.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setLabels(new ArrayList<>());
        task.setIsBlocker(false);
        long taskId = this.taskRepository.saveAndFlush(task).getId();

        String payload = """
            {
              "type": "static",
              "organizationId": "%s"
            }
            """.formatted(inaccessibleOrganizationId);

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(createJwt(account.getSubject()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:access-denied"))
            .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void updateTask_Dynamic_PreservesExistingDependenciesWhenOmitted() throws Exception {
        Account account = createAccount();

        long predecessorId = createDynamicPredecessorTask(account.getIdentity());

        DynamicTask task = new DynamicTask();
        task.setIdentity(account.getIdentity());
        task.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        task.setName("Original dynamic task");
        task.setDescription("Original dynamic description");
        task.setDifficulty(3);
        task.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        task.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        task.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        task.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        task.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
        task.setLabels(new ArrayList<>());
        task.setScopes(new ArrayList<>());
        task.setDependencies(new HashSet<>(List.of((DynamicTask) taskRepository.getReferenceById(predecessorId))));
        task.setDependents(new HashSet<>());
        long taskId = this.taskRepository.saveAndFlush(task).getId();

        String payload = """
            {
              "type": "dynamic",
              "description": "Updated dynamic description"
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(taskId))
            .andExpect(jsonPath("$.type").value("dynamic"))
            .andExpect(jsonPath("$.description").value("Updated dynamic description"))
            .andExpect(jsonPath("$.dependencies.length()").value(1))
            .andExpect(jsonPath("$.dependencies[0]").value(predecessorId));
    }

    @Test
    void updateTask_Static_DifficultyAboveFive_ReturnsValidationError() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();
        

        StaticTask task = new StaticTask();
        task.setIdentity(account.getIdentity());
        task.setOrganizationId(organizationId);
        task.setName("Original static task");
        task.setDescription("Original description");
        task.setDifficulty(2);
        task.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setLabels(new ArrayList<>());
        task.setIsBlocker(false);
        long taskId = taskRepository.saveAndFlush(task).getId();

        String payload = """
            {
              "type": "static",
              "difficulty": 6
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("difficulty")));
    }

    @Test
    void updateTask_Dynamic_ReducesDuration_CapsMinAndMaxScopeDurations() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();
        

        DynamicTask task = new DynamicTask();
        task.setIdentity(account.getIdentity());
        task.setOrganizationId(organizationId);
        task.setName("Dynamic task");
        task.setDescription("Should cap scope durations when duration is lowered");
        task.setDifficulty(3);
        task.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        task.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        task.setElapsed(Duration.of(10, ChronoUnit.MINUTES));
        task.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        task.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
        task.setLabels(new ArrayList<>());
        task.setScopes(new ArrayList<>());
        task.setDependencies(new HashSet<>());
        task.setDependents(new HashSet<>());
        long taskId = taskRepository.saveAndFlush(task).getId();

        String payload = """
            {
              "type": "dynamic",
              "duration": "PT8M"
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.duration").value("PT8M"))
            .andExpect(jsonPath("$.minScopeDuration").value("PT8M"))
            .andExpect(jsonPath("$.maxScopeDuration").value("PT8M"));
    }

    @Test
    void updateTask_Dynamic_AllowsElapsedZero() throws Exception {
        Account account = createAccount();

        DynamicTask task = new DynamicTask();
        task.setIdentity(account.getIdentity());
        task.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        task.setName("Dynamic task");
        task.setDescription("Can reset elapsed to zero");
        task.setDifficulty(3);
        task.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        task.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        task.setElapsed(Duration.of(45, ChronoUnit.MINUTES));
        task.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        task.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
        task.setLabels(new ArrayList<>());
        task.setScopes(new ArrayList<>());
        task.setDependencies(new HashSet<>());
        task.setDependents(new HashSet<>());
        long taskId = this.taskRepository.saveAndFlush(task).getId();

        String payload = """
            {
              "type": "dynamic",
              "elapsed": 0
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(taskId))
            .andExpect(jsonPath("$.type").value("dynamic"))
            .andExpect(jsonPath("$.elapsed").value("PT0S"));

        DynamicTask updatedTask = (DynamicTask) taskRepository.findById(taskId).orElseThrow();
        assertTrue(updatedTask.getElapsed().isZero());
    }

    @Test
    void updateTask_Dynamic_NegativeElapsed_ReturnsValidationError() throws Exception {
        Account account = createAccount();

        DynamicTask task = new DynamicTask();
        task.setIdentity(account.getIdentity());
        task.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        task.setName("Dynamic task");
        task.setDescription("Should reject negative elapsed");
        task.setDifficulty(3);
        task.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        task.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        task.setElapsed(Duration.of(10, ChronoUnit.MINUTES));
        task.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        task.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
        task.setLabels(new ArrayList<>());
        task.setScopes(new ArrayList<>());
        task.setDependencies(new HashSet<>());
        task.setDependents(new HashSet<>());
        long taskId = this.taskRepository.saveAndFlush(task).getId();

        String payload = """
            {
              "type": "dynamic",
              "elapsed": -1
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("elapsedValid")));
    }

    @Test
    void updateTask_Dynamic_WithStaticTypePayload_ReturnsValidationError() throws Exception {
        Account account = createAccount();

        DynamicTask task = new DynamicTask();
        task.setIdentity(account.getIdentity());
        task.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        task.setName("Dynamic task");
        task.setDescription("Type mismatch case");
        task.setDifficulty(3);
        task.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        task.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        task.setElapsed(Duration.of(10, ChronoUnit.MINUTES));
        task.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        task.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
        task.setLabels(new ArrayList<>());
        task.setScopes(new ArrayList<>());
        task.setDependencies(new HashSet<>());
        task.setDependents(new HashSet<>());
        long taskId = this.taskRepository.saveAndFlush(task).getId();

        String payload = """
            {
              "type": "static",
              "name": "Attempted static update"
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.detail").value("Task type mismatch: expected static task"));
    }

    @Test
    void updateTask_Dynamic_RemovesDependencyAndCleansInverseRelation() throws Exception {
        Account account = createAccount();

        long predecessorId = createDynamicPredecessorTask(account.getIdentity());

        DynamicTask dependent = new DynamicTask();
        dependent.setIdentity(account.getIdentity());
        dependent.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        dependent.setName("Dependent task");
        dependent.setDescription("Initially depends on predecessor");
        dependent.setDifficulty(3);
        dependent.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        dependent.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        dependent.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        dependent.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        dependent.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        dependent.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
        dependent.setLabels(new ArrayList<>());
        dependent.setScopes(new ArrayList<>());
        dependent.setDependencies(new HashSet<>(List.of((DynamicTask) taskRepository.getReferenceById(predecessorId))));
        dependent.setDependents(new HashSet<>());
        long dependentId = this.taskRepository.saveAndFlush(dependent).getId();

        String payload = """
            {
              "type": "dynamic",
              "dependencies": []
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", dependentId)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(dependentId))
            .andExpect(jsonPath("$.type").value("dynamic"))
            .andExpect(jsonPath("$.dependencies").isArray())
            .andExpect(jsonPath("$.dependencies").isEmpty());

        entityManager.flush();
        entityManager.clear();

        DynamicTask reloadedDependent = (DynamicTask) taskRepository.findById(dependentId).orElseThrow();
        DynamicTask reloadedPredecessor = (DynamicTask) taskRepository.findById(predecessorId).orElseThrow();

        assertTrue(reloadedDependent.getDependencies().isEmpty());
        assertTrue(reloadedPredecessor.getDependents().isEmpty());
    }

    @Test
    void updateTask_Dynamic_AddsDependencyAndCleansInverseRelation() throws Exception {
        Account account = createAccount();

        long dependencyId = createDynamicPredecessorTask(account.getIdentity());

        DynamicTask dependent = new DynamicTask();
        dependent.setIdentity(account.getIdentity());
        dependent.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        dependent.setName("Independent task");
        dependent.setDescription("Will gain a dependency");
        dependent.setDifficulty(3);
        dependent.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        dependent.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        dependent.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        dependent.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        dependent.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        dependent.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
        dependent.setLabels(new ArrayList<>());
        dependent.setScopes(new ArrayList<>());
        dependent.setDependencies(new HashSet<>());
        dependent.setDependents(new HashSet<>());
        long dependentId = this.taskRepository.saveAndFlush(dependent).getId();

        String payload = """
            {
              "type": "dynamic",
              "dependencies": [%d]
            }
            """.formatted(dependencyId);

        mockMvc.perform(patch("/v1/tasks/{id}", dependentId)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(dependentId))
            .andExpect(jsonPath("$.type").value("dynamic"))
            .andExpect(jsonPath("$.dependencies.length()").value(1))
            .andExpect(jsonPath("$.dependencies[0]").value(dependencyId));

        entityManager.flush();
        entityManager.clear();

        DynamicTask reloadedDependent = (DynamicTask) taskRepository.findById(dependentId).orElseThrow();
        DynamicTask reloadedDependency = (DynamicTask) taskRepository.findById(dependencyId).orElseThrow();

        assertTrue(reloadedDependent.getDependencies().stream().anyMatch(task -> task.getId().equals(dependencyId)));
        assertTrue(reloadedDependency.getDependents().stream().anyMatch(task -> task.getId().equals(dependentId)));
    }

    @Test
    void updateTask_Dynamic_WithDependencyFromDifferentIdentity_ReturnsValidationError() throws Exception {
        Identity identity = new Identity();
        Identity otherIdentity = new Identity();
        Account account = this.createAccount(identity);
        Account otherAccount = this.createAccount(otherIdentity);
        long foreignDependencyId = createDynamicPredecessorTask(otherAccount.getIdentity());

        DynamicTask task = new DynamicTask();
        task.setIdentity(account.getIdentity());
        task.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        task.setName("Task to patch");
        task.setDescription("Should reject foreign dependency");
        task.setDifficulty(3);
        task.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        task.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        task.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        task.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        task.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
        task.setLabels(new ArrayList<>());
        task.setScopes(new ArrayList<>());
        task.setDependencies(new HashSet<>());
        task.setDependents(new HashSet<>());
        long taskId = this.taskRepository.saveAndFlush(task).getId();

        String payload = """
            {
              "type": "dynamic",
              "dependencies": [%d]
            }
            """.formatted(foreignDependencyId);

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.detail").value("Dependency task " + foreignDependencyId + " must belong to the same identity"));
    }

    @Test
    void updateTask_FromDifferentIdentity_Returns404() throws Exception {
        Account ownerAccount = createAccount();

        StaticTask task = new StaticTask();
        task.setIdentity(ownerAccount.getIdentity());
        task.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        task.setName("Foreign task");
        task.setDescription("Should not be patchable by someone else");
        task.setDifficulty(1);
        task.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setLabels(new ArrayList<>());
        task.setIsBlocker(false);
        long taskId = this.taskRepository.saveAndFlush(task).getId();

        String attackerSubject = UUID.randomUUID().toString();

        String payload = """
            {
              "type": "static",
              "name": "Updated name"
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(attackerSubject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateTask_NotFound_Returns404() throws Exception {
        Account account = createAccount();

        String payload = """
            {
              "type": "static",
              "name": "Updated name"
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", 999_999L)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isNotFound());
    }

    @Test
    void createTask_Dynamic_MissingDependencies_ReturnsValidationError() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();
        

        String payload = """
            {
              "type": "dynamic",
              "organizationId": "%s",
              "name": "Implement API endpoint",
              "description": "Create and test endpoint",
              "difficulty": 4,
              "startAt": "2026-04-20T08:00:00Z",
              "endAt": "2026-04-25T18:00:00Z",
              "labels": [],
              "duration": "PT240M",
              "minScopeDuration": "PT30M",
              "maxScopeDuration": "PT90M"
            }
            """.formatted(organizationId);

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("dependencies")));
    }

    @Test
    void createTask_Static_MissingStartAndEndAt_ReturnsValidationError() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();
        

        String payload = """
            {
              "type": "static",
              "organizationId": "%s",
              "name": "Write report",
              "description": "Prepare weekly summary",
              "rrule": "FREQ=WEEKLY;BYDAY=MO",
              "difficulty": 3,
              "labels": [],
              "isBlocker": false
            }
            """.formatted(organizationId);

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.title").value("Validation Failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Request validation failed"))
            .andExpect(jsonPath("$.instance").value("/v1/tasks"))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("startAt")))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("endAt")));
    }

    @Test
    void createTask_Static_MissingDescriptionRruleAndLabels_ReturnsValidationError() throws Exception {
        Account account = createAccount();
        String organizationId = UUID.randomUUID().toString();

        String payload = """
            {
              "type": "static",
              "organizationId": "%s",
              "name": "Write report",
              "difficulty": 3,
              "startAt": "2026-04-20T09:00:00Z",
              "endAt": "2026-04-20T10:00:00Z",
              "isBlocker": false
            }
            """.formatted(organizationId);

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("description")))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("rrule")))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("labels")));
    }

    @Test
    void deleteTask_Static_ReturnsNoContentAndRemovesTask() throws Exception {
        Account account = createAccount();

        StaticTask task = new StaticTask();
        task.setIdentity(account.getIdentity());
        task.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        task.setName("Static to delete");
        task.setDescription("Delete me");
        task.setDifficulty(1);
        task.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setLabels(new ArrayList<>());
        task.setIsBlocker(false);
        long taskId = this.taskRepository.saveAndFlush(task).getId();

        mockMvc.perform(delete("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject()))))
            .andExpect(status().isNoContent());

        assertTrue(taskRepository.findById(taskId).isEmpty());
    }

    @Test
    void deleteTask_Dynamic_RemovesScopesAndInboundLinks() throws Exception {
        Account account = createAccount();

        DynamicTask predecessor = new DynamicTask();
        predecessor.setIdentity(account.getIdentity());
        predecessor.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        predecessor.setName("Predecessor");
        predecessor.setDescription("Will be deleted");
        predecessor.setDifficulty(2);
        predecessor.setStartAt(Instant.parse("2026-04-20T08:00:00Z"));
        predecessor.setEndAt(Instant.parse("2026-04-22T18:00:00Z"));
        predecessor.setDuration(Duration.of(120, ChronoUnit.MINUTES));
        predecessor.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        predecessor.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        predecessor.setMaxScopeDuration(Duration.of(60, ChronoUnit.MINUTES));
        predecessor.setLabels(new ArrayList<>());
        predecessor.setDependencies(new HashSet<>());
        predecessor.setDependents(new HashSet<>());

        Scope scope = new Scope();
        scope.setDynamicTask(predecessor);
        scope.setStartAt(Instant.parse("2026-04-20T08:00:00Z"));
        scope.setEndAt(Instant.parse("2026-04-20T09:00:00Z"));
        predecessor.setScopes(new ArrayList<>(List.of(scope)));

        predecessor = this.taskRepository.saveAndFlush(predecessor);
        long predecessorId = predecessor.getId();
        long predecessorScopeId = predecessor.getScopes().getFirst().getId();

        DynamicTask dependent = new DynamicTask();
        dependent.setIdentity(account.getIdentity());
        dependent.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        dependent.setName("Dependent");
        dependent.setDescription("Depends on predecessor");
        dependent.setDifficulty(3);
        dependent.setStartAt(Instant.parse("2026-04-20T08:00:00Z"));
        dependent.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        dependent.setDuration(Duration.of(240, ChronoUnit.MINUTES));
        dependent.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        dependent.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        dependent.setMaxScopeDuration(Duration.of(120, ChronoUnit.MINUTES));
        dependent.setLabels(new ArrayList<>());
        dependent.setScopes(new ArrayList<>());
        dependent.setDependencies(new HashSet<>(List.of(predecessor)));
        dependent.setDependents(new HashSet<>());

        this.taskRepository.saveAndFlush(dependent);

        mockMvc.perform(delete("/v1/tasks/{id}", predecessorId)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject()))))
            .andExpect(status().isNoContent());

        assertTrue(taskRepository.findById(predecessorId).isEmpty());
        assertTrue(scopeRepository.findById(predecessorScopeId).isEmpty());

        DynamicTask updatedDependent = (DynamicTask) taskRepository.findById(dependent.getId()).orElseThrow();
        assertTrue(updatedDependent.getDependencies().isEmpty());
    }

    @Test
    void deleteTask_NotFound_Returns404() throws Exception {
        Account account = createAccount();

        mockMvc.perform(delete("/v1/tasks/{id}", 999_999L)
                .with(jwt().jwt(jwt -> jwt.subject(account.getSubject()))))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_FromDifferentIdentity_Returns404() throws Exception {
        Account account = createAccount();

        StaticTask task = new StaticTask();
        task.setIdentity(account.getIdentity());
        task.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        task.setName("Foreign task");
        task.setDescription("Should not be deletable");
        task.setDifficulty(1);
        task.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setLabels(new ArrayList<>());
        task.setIsBlocker(false);
        long taskId = this.taskRepository.saveAndFlush(task).getId();

        String attackerSubject = UUID.randomUUID().toString();

        mockMvc.perform(delete("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(attackerSubject))))
            .andExpect(status().isNotFound());
    }
}
