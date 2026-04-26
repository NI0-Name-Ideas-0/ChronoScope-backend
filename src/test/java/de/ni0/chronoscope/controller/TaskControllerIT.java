package de.ni0.chronoscope.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskControllerIT {

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

    @PersistenceContext
    private EntityManager entityManager;

    private long createAccount(String subject) {
        Identity identity = identityRepository.saveAndFlush(new Identity());
        return createAccount(identity.getId(), subject);
    }

    private long createAccount(long identityId, String subject) {
        Account account = new Account();
        account.setIdentity(identityRepository.getReferenceById(identityId));
        account.setSubject(subject);
        return accountRepository.saveAndFlush(account).getId();
    }

    private String createAccountSubject() {
        return "it-subject-" + System.nanoTime();
    }

    private long createDynamicPredecessorTask(long accountId) {
        DynamicTask predecessor = new DynamicTask();
        predecessor.setAccount(accountRepository.getReferenceById(accountId));
        predecessor.setName("Predecessor task");
        predecessor.setDescription("Predecessor task for dependency test");
        predecessor.setDifficulty(2);
        predecessor.setStartAt(Instant.parse("2026-04-20T08:00:00Z"));
        predecessor.setEndAt(Instant.parse("2026-04-22T18:00:00Z"));
        predecessor.setRrule("FREQ=DAILY");
        predecessor.setDuration(Duration.of(120, ChronoUnit.MINUTES));
        predecessor.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        predecessor.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        predecessor.setMaxScopeDuration(Duration.of(60, ChronoUnit.MINUTES));
        predecessor.setLabels(new ArrayList<>());
        predecessor.setScopes(new ArrayList<>());
        predecessor.setDependencies(new HashSet<>());
        predecessor.setDependents(new HashSet<>());
        return taskRepository.saveAndFlush(predecessor).getId();
    }

    @Test
    void getTasks_ReturnsTasksFromCurrentIdentityAndLinkedAccountsOnly() throws Exception {
        Identity identity = identityRepository.saveAndFlush(new Identity());
        String subject = "it-get-tasks-subject-" + System.nanoTime();

        long primaryAccountId = createAccount(identity.getId(), subject);
        long linkedAccountId = createAccount(identity.getId(), "it-linked-subject-" + System.nanoTime());

        Identity otherIdentity = identityRepository.saveAndFlush(new Identity());
        long otherAccountId = createAccount(otherIdentity.getId(), "it-other-subject-" + System.nanoTime());

        String dynamicTaskName = "it-dynamic-task-" + System.nanoTime();
        String linkedStaticTaskName = "it-linked-static-task-" + System.nanoTime();
        String foreignTaskName = "it-foreign-task-" + System.nanoTime();

        DynamicTask ownTask = new DynamicTask();
        ownTask.setAccount(accountRepository.getReferenceById(primaryAccountId));
        ownTask.setName(dynamicTaskName);
        ownTask.setDescription("Dynamic task in current identity");
        ownTask.setDifficulty(2);
        ownTask.setStartAt(Instant.parse("2026-04-20T08:00:00Z"));
        ownTask.setEndAt(Instant.parse("2026-04-22T18:00:00Z"));
        ownTask.setRrule("FREQ=DAILY");
        ownTask.setDuration(Duration.of(120, ChronoUnit.MINUTES));
        ownTask.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        ownTask.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        ownTask.setMaxScopeDuration(Duration.of(60, ChronoUnit.MINUTES));
        ownTask.setLabels(new ArrayList<>());
        ownTask.setScopes(new ArrayList<>());
        ownTask.setDependencies(new HashSet<>());
        ownTask.setDependents(new HashSet<>());
        taskRepository.saveAndFlush(ownTask);

        StaticTask linkedTask = new StaticTask();
        linkedTask.setAccount(accountRepository.getReferenceById(linkedAccountId));
        linkedTask.setName(linkedStaticTaskName);
        linkedTask.setDescription("Static task in linked account");
        linkedTask.setDifficulty(1);
        linkedTask.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        linkedTask.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        linkedTask.setRrule("FREQ=DAILY");
        linkedTask.setLabels(new ArrayList<>());
        linkedTask.setIsBlocker(false);
        taskRepository.saveAndFlush(linkedTask);

        StaticTask foreignTask = new StaticTask();
        foreignTask.setAccount(accountRepository.getReferenceById(otherAccountId));
        foreignTask.setName(foreignTaskName);
        foreignTask.setDescription("Task from another identity");
        foreignTask.setDifficulty(1);
        foreignTask.setStartAt(Instant.parse("2026-04-20T11:00:00Z"));
        foreignTask.setEndAt(Instant.parse("2026-04-20T12:00:00Z"));
        foreignTask.setRrule("FREQ=DAILY");
        foreignTask.setLabels(new ArrayList<>());
        foreignTask.setIsBlocker(false);
        taskRepository.saveAndFlush(foreignTask);

        mockMvc.perform(get("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt
                    .subject(subject)
                    .claim("organization", List.of("private")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", hasItem(dynamicTaskName)))
            .andExpect(jsonPath("$[*].name", hasItem(linkedStaticTaskName)))
            .andExpect(jsonPath("$[*].name", not(hasItem(foreignTaskName))))
            .andExpect(jsonPath("$[*].accountId", hasItem((int) primaryAccountId)))
            .andExpect(jsonPath("$[*].accountId", hasItem((int) linkedAccountId)))
            .andExpect(jsonPath("$[*].type", hasItem("dynamic")))
            .andExpect(jsonPath("$[*].type", hasItem("static")));
    }

    @Test
    void createTask_Static_ReturnsCreated() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        String payload = """
            {
              "type": "static",
              "accountId": %d,
              "name": "Write report",
              "description": "Prepare weekly summary",
              "rrule": "FREQ=WEEKLY;BYDAY=MO",
              "difficulty": 3,
              "startAt": "2026-04-20T09:00:00Z",
              "endAt": "2026-04-20T10:00:00Z",
              "labels": [],
              "isBlocker": false
            }
            """.formatted(accountId);

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountId").value(accountId))
            .andExpect(jsonPath("$.name").value("Write report"))
            .andExpect(jsonPath("$.description").value("Prepare weekly summary"))
            .andExpect(jsonPath("$.difficulty").value(3))
            .andExpect(jsonPath("$.rrule").value("FREQ=WEEKLY;BYDAY=MO"))
            .andExpect(jsonPath("$.isBlocker").value(false));
    }

    @Test
    void createTask_Static_DifficultyAboveFive_ReturnsValidationError() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        String payload = """
            {
              "type": "static",
              "accountId": %d,
              "name": "Write report",
              "description": "Prepare weekly summary",
              "rrule": "FREQ=WEEKLY;BYDAY=MO",
              "difficulty": 6,
              "startAt": "2026-04-20T09:00:00Z",
              "endAt": "2026-04-20T10:00:00Z",
              "labels": [],
              "isBlocker": false
            }
            """.formatted(accountId);

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("difficulty")));
    }

    @Test
    void createTask_Dynamic_ReturnsCreatedWithDefaults() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        String payload = """
            {
              "type": "dynamic",
              "accountId": %d,
              "name": "Implement API endpoint",
              "description": "Create and test endpoint",
              "rrule": "FREQ=DAILY",
              "difficulty": 4,
              "startAt": "2026-04-20T08:00:00Z",
              "endAt": "2026-04-25T18:00:00Z",
              "labels": [],
              "duration": "PT240M",
              "minScopeDuration": "PT30M",
                            "maxScopeDuration": "PT90M",
              "dependencies": []
            }
            """.formatted(accountId);

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountId").value(accountId))
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
    void createTask_Dynamic_WithStartAtAfterEndAt_ReturnsValidationError() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        String payload = """
            {
              "type": "dynamic",
              "accountId": %d,
              "name": "Implement API endpoint",
              "description": "Create and test endpoint",
              "rrule": "FREQ=DAILY",
              "difficulty": 4,
              "startAt": "2026-04-25T18:00:00Z",
              "endAt": "2026-04-20T08:00:00Z",
              "labels": [],
              "duration": "PT240M",
              "minScopeDuration": "PT30M",
                            "maxScopeDuration": "PT90M",
              "dependencies": []
            }
            """.formatted(accountId);

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.detail").value("startAt must be before endAt"));
    }

    @Test
    void createTask_Dynamic_InvalidDurationFormat_ReturnsInvalidRequest() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        String payload = """
            {
              "type": "dynamic",
              "accountId": %d,
              "name": "Implement API endpoint",
              "description": "Create and test endpoint",
              "rrule": "FREQ=DAILY",
              "difficulty": 4,
              "startAt": "2026-04-20T08:00:00Z",
              "endAt": "2026-04-25T18:00:00Z",
              "labels": [],
              "duration": "not-a-duration",
              "minScopeDuration": "PT30M",
              "maxScopeDuration": "PT90M",
              "dependencies": []
            }
            """.formatted(accountId);

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
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
        String subject = createAccountSubject();

        String payload = """
            {
              "type": "static",
              "name": "Write report"
            """;

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
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
        String subject = createAccountSubject();
        long accountId = createAccount(subject);
        long predecessorId = createDynamicPredecessorTask(accountId);

        String payload = """
            {
              "type": "dynamic",
              "accountId": %d,
              "name": "Task with dependencies",
              "description": "Should link predecessor",
              "rrule": "FREQ=DAILY",
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
            """.formatted(accountId, predecessorId);

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
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
        void createTask_Dynamic_WithDependencyFromDifferentAccountSameIdentity_ReturnsCreated() throws Exception {
                Identity identity = identityRepository.saveAndFlush(new Identity());
                String requesterSubject = createAccountSubject();
                long requesterAccountId = createAccount(identity.getId(), requesterSubject);
                long linkedAccountId = createAccount(identity.getId(), createAccountSubject());
                long predecessorId = createDynamicPredecessorTask(linkedAccountId);

                String payload = """
                        {
                            "type": "dynamic",
                            "accountId": %d,
                            "name": "Cross-account dependency",
                            "description": "Dependency should be allowed within same identity",
                            "rrule": "FREQ=DAILY",
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
                        """.formatted(requesterAccountId, predecessorId);

                mockMvc.perform(post("/v1/tasks")
                                .with(jwt().jwt(jwt -> jwt.subject(requesterSubject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.accountId").value(requesterAccountId))
                        .andExpect(jsonPath("$.dependencies").isArray())
                        .andExpect(jsonPath("$.dependencies.length()").value(1))
                        .andExpect(jsonPath("$.dependencies[0]").value(predecessorId));
        }

        @Test
        void createTask_Dynamic_WithDependencyFromDifferentIdentity_ReturnsValidationError() throws Exception {
                Identity sourceIdentity = identityRepository.saveAndFlush(new Identity());
                String requesterSubject = createAccountSubject();
                long requesterAccountId = createAccount(sourceIdentity.getId(), requesterSubject);

                Identity otherIdentity = identityRepository.saveAndFlush(new Identity());
                long foreignAccountId = createAccount(otherIdentity.getId(), createAccountSubject());
                long predecessorId = createDynamicPredecessorTask(foreignAccountId);

                String payload = """
                        {
                            "type": "dynamic",
                            "accountId": %d,
                            "name": "Cross-identity dependency",
                            "description": "Dependency should be rejected",
                            "rrule": "FREQ=DAILY",
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
                        """.formatted(requesterAccountId, predecessorId);

                mockMvc.perform(post("/v1/tasks")
                                .with(jwt().jwt(jwt -> jwt.subject(requesterSubject)))
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
            String subject = createAccountSubject();
            long accountId = createAccount(subject);
            long predecessorId = createDynamicPredecessorTask(accountId);

            DynamicTask dependent = new DynamicTask();
            dependent.setAccount(accountRepository.getReferenceById(accountId));
            dependent.setName("Dependent dynamic task");
            dependent.setDescription("Depends on predecessor");
            dependent.setDifficulty(3);
            dependent.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
            dependent.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
            dependent.setRrule("FREQ=DAILY");
            dependent.setDuration(Duration.of(180, ChronoUnit.MINUTES));
            dependent.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
            dependent.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
            dependent.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
            dependent.setLabels(new ArrayList<>());
            dependent.setScopes(new ArrayList<>());
            dependent.setDependencies(new HashSet<>(List.of((DynamicTask) taskRepository.getReferenceById(predecessorId))));
            dependent.setDependents(new HashSet<>());
            dependent = taskRepository.saveAndFlush(dependent);

            mockMvc.perform(get("/v1/tasks/{id}", dependent.getId())
                .with(jwt().jwt(jwt -> jwt.subject(subject))))
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
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        StaticTask task = new StaticTask();
        task.setAccount(accountRepository.getReferenceById(accountId));
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
              "name": "Updated static task"
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
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
    void updateTask_Dynamic_PreservesExistingDependenciesWhenOmitted() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        long predecessorId = createDynamicPredecessorTask(accountId);

        DynamicTask task = new DynamicTask();
        task.setAccount(accountRepository.getReferenceById(accountId));
        task.setName("Original dynamic task");
        task.setDescription("Original dynamic description");
        task.setDifficulty(3);
        task.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        task.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        task.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        task.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
        task.setLabels(new ArrayList<>());
        task.setScopes(new ArrayList<>());
        task.setDependencies(new HashSet<>(List.of((DynamicTask) taskRepository.getReferenceById(predecessorId))));
        task.setDependents(new HashSet<>());
        long taskId = taskRepository.saveAndFlush(task).getId();

        String payload = """
            {
              "type": "dynamic",
              "description": "Updated dynamic description"
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
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
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        StaticTask task = new StaticTask();
        task.setAccount(accountRepository.getReferenceById(accountId));
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
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("difficulty")));
    }

    @Test
    void updateTask_Dynamic_ReducesDuration_CapsMinAndMaxScopeDurations() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        DynamicTask task = new DynamicTask();
        task.setAccount(accountRepository.getReferenceById(accountId));
        task.setName("Dynamic task");
        task.setDescription("Should cap scope durations when duration is lowered");
        task.setDifficulty(3);
        task.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        task.setRrule("FREQ=DAILY");
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
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.duration").value("PT8M"))
            .andExpect(jsonPath("$.minScopeDuration").value("PT8M"))
            .andExpect(jsonPath("$.maxScopeDuration").value("PT8M"));
    }

    @Test
    void updateTask_Dynamic_AllowsElapsedZero() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        DynamicTask task = new DynamicTask();
        task.setAccount(accountRepository.getReferenceById(accountId));
        task.setName("Dynamic task");
        task.setDescription("Can reset elapsed to zero");
        task.setDifficulty(3);
        task.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        task.setElapsed(Duration.of(45, ChronoUnit.MINUTES));
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
              "elapsed": 0
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
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
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        DynamicTask task = new DynamicTask();
        task.setAccount(accountRepository.getReferenceById(accountId));
        task.setName("Dynamic task");
        task.setDescription("Should reject negative elapsed");
        task.setDifficulty(3);
        task.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        task.setRrule("FREQ=DAILY");
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
              "elapsed": -1
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("elapsedValid")));
    }

    @Test
    void updateTask_Dynamic_WithStaticTypePayload_ReturnsValidationError() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        DynamicTask task = new DynamicTask();
        task.setAccount(accountRepository.getReferenceById(accountId));
        task.setName("Dynamic task");
        task.setDescription("Type mismatch case");
        task.setDifficulty(3);
        task.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        task.setRrule("FREQ=DAILY");
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
              "type": "static",
              "name": "Attempted static update"
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.detail").value("Task type mismatch: expected static task"));
    }

    @Test
    void updateTask_Dynamic_RemovesDependencyAndCleansInverseRelation() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        long predecessorId = createDynamicPredecessorTask(accountId);

        DynamicTask dependent = new DynamicTask();
        dependent.setAccount(accountRepository.getReferenceById(accountId));
        dependent.setName("Dependent task");
        dependent.setDescription("Initially depends on predecessor");
        dependent.setDifficulty(3);
        dependent.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        dependent.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        dependent.setRrule("FREQ=DAILY");
        dependent.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        dependent.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        dependent.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        dependent.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
        dependent.setLabels(new ArrayList<>());
        dependent.setScopes(new ArrayList<>());
        dependent.setDependencies(new HashSet<>(List.of((DynamicTask) taskRepository.getReferenceById(predecessorId))));
        dependent.setDependents(new HashSet<>());
        long dependentId = taskRepository.saveAndFlush(dependent).getId();

        String payload = """
            {
              "type": "dynamic",
              "dependencies": []
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", dependentId)
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
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
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        long dependencyId = createDynamicPredecessorTask(accountId);

        DynamicTask dependent = new DynamicTask();
        dependent.setAccount(accountRepository.getReferenceById(accountId));
        dependent.setName("Independent task");
        dependent.setDescription("Will gain a dependency");
        dependent.setDifficulty(3);
        dependent.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        dependent.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        dependent.setRrule("FREQ=DAILY");
        dependent.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        dependent.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        dependent.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        dependent.setMaxScopeDuration(Duration.of(90, ChronoUnit.MINUTES));
        dependent.setLabels(new ArrayList<>());
        dependent.setScopes(new ArrayList<>());
        dependent.setDependencies(new HashSet<>());
        dependent.setDependents(new HashSet<>());
        long dependentId = taskRepository.saveAndFlush(dependent).getId();

        String payload = """
            {
              "type": "dynamic",
              "dependencies": [%d]
            }
            """.formatted(dependencyId);

        mockMvc.perform(patch("/v1/tasks/{id}", dependentId)
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
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
        Identity sourceIdentity = identityRepository.saveAndFlush(new Identity());
        String requesterSubject = createAccountSubject();
        long requesterAccountId = createAccount(sourceIdentity.getId(), requesterSubject);

        Identity otherIdentity = identityRepository.saveAndFlush(new Identity());
        long foreignAccountId = createAccount(otherIdentity.getId(), createAccountSubject());
        long foreignDependencyId = createDynamicPredecessorTask(foreignAccountId);

        DynamicTask task = new DynamicTask();
        task.setAccount(accountRepository.getReferenceById(requesterAccountId));
        task.setName("Task to patch");
        task.setDescription("Should reject foreign dependency");
        task.setDifficulty(3);
        task.setStartAt(Instant.parse("2026-04-21T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setDuration(Duration.of(180, ChronoUnit.MINUTES));
        task.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
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
              "dependencies": [%d]
            }
            """.formatted(foreignDependencyId);

        mockMvc.perform(patch("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(requesterSubject)))
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
        String ownerSubject = createAccountSubject();
        long ownerAccountId = createAccount(ownerSubject);

        StaticTask task = new StaticTask();
        task.setAccount(accountRepository.getReferenceById(ownerAccountId));
        task.setName("Foreign task");
        task.setDescription("Should not be patchable by someone else");
        task.setDifficulty(1);
        task.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setLabels(new ArrayList<>());
        task.setIsBlocker(false);
        long taskId = taskRepository.saveAndFlush(task).getId();

        String attackerSubject = createAccountSubject();
        createAccount(attackerSubject);

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
        String subject = createAccountSubject();
        createAccount(subject);

        String payload = """
            {
              "type": "static",
              "name": "Updated name"
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", 999_999L)
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isNotFound());
    }

    @Test
    void createTask_Dynamic_MissingDependencies_ReturnsValidationError() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        String payload = """
            {
              "type": "dynamic",
              "accountId": %d,
              "name": "Implement API endpoint",
              "description": "Create and test endpoint",
              "rrule": "FREQ=DAILY",
              "difficulty": 4,
              "startAt": "2026-04-20T08:00:00Z",
              "endAt": "2026-04-25T18:00:00Z",
              "labels": [],
              "duration": "PT240M",
              "minScopeDuration": "PT30M",
              "maxScopeDuration": "PT90M"
            }
            """.formatted(accountId);

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("dependencies")));
    }

    @Test
    void createTask_Static_MissingStartAndEndAt_ReturnsValidationError() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        String payload = """
            {
              "type": "static",
              "accountId": %d,
              "name": "Write report",
              "description": "Prepare weekly summary",
              "rrule": "FREQ=WEEKLY;BYDAY=MO",
              "difficulty": 3,
              "labels": [],
              "isBlocker": false
            }
            """.formatted(accountId);

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
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
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        String payload = """
            {
              "type": "static",
              "accountId": %d,
              "name": "Write report",
              "difficulty": 3,
              "startAt": "2026-04-20T09:00:00Z",
              "endAt": "2026-04-20T10:00:00Z",
              "isBlocker": false
            }
            """.formatted(accountId);

        mockMvc.perform(post("/v1/tasks")
                .with(jwt().jwt(jwt -> jwt.subject(subject)))
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
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        StaticTask task = new StaticTask();
        task.setAccount(accountRepository.getReferenceById(accountId));
        task.setName("Static to delete");
        task.setDescription("Delete me");
        task.setDifficulty(1);
        task.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setLabels(new ArrayList<>());
        task.setIsBlocker(false);
        long taskId = taskRepository.saveAndFlush(task).getId();

        mockMvc.perform(delete("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(subject))))
            .andExpect(status().isNoContent());

        assertTrue(taskRepository.findById(taskId).isEmpty());
    }

    @Test
    void deleteTask_Dynamic_RemovesScopesAndInboundLinks() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);

        DynamicTask predecessor = new DynamicTask();
        predecessor.setAccount(accountRepository.getReferenceById(accountId));
        predecessor.setName("Predecessor");
        predecessor.setDescription("Will be deleted");
        predecessor.setDifficulty(2);
        predecessor.setStartAt(Instant.parse("2026-04-20T08:00:00Z"));
        predecessor.setEndAt(Instant.parse("2026-04-22T18:00:00Z"));
        predecessor.setRrule("FREQ=DAILY");
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

        predecessor = taskRepository.saveAndFlush(predecessor);
        long predecessorId = predecessor.getId();
        long predecessorScopeId = predecessor.getScopes().getFirst().getId();

        DynamicTask dependent = new DynamicTask();
        dependent.setAccount(accountRepository.getReferenceById(accountId));
        dependent.setName("Dependent");
        dependent.setDescription("Depends on predecessor");
        dependent.setDifficulty(3);
        dependent.setStartAt(Instant.parse("2026-04-20T08:00:00Z"));
        dependent.setEndAt(Instant.parse("2026-04-24T18:00:00Z"));
        dependent.setRrule("FREQ=DAILY");
        dependent.setDuration(Duration.of(240, ChronoUnit.MINUTES));
        dependent.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        dependent.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        dependent.setMaxScopeDuration(Duration.of(120, ChronoUnit.MINUTES));
        dependent.setLabels(new ArrayList<>());
        dependent.setScopes(new ArrayList<>());
        dependent.setDependencies(new HashSet<>(List.of(predecessor)));
        dependent.setDependents(new HashSet<>());

        taskRepository.saveAndFlush(dependent);

        mockMvc.perform(delete("/v1/tasks/{id}", predecessorId)
                .with(jwt().jwt(jwt -> jwt.subject(subject))))
            .andExpect(status().isNoContent());

        assertTrue(taskRepository.findById(predecessorId).isEmpty());
        assertTrue(scopeRepository.findById(predecessorScopeId).isEmpty());

        DynamicTask updatedDependent = (DynamicTask) taskRepository.findById(dependent.getId()).orElseThrow();
        assertTrue(updatedDependent.getDependencies().isEmpty());
    }

    @Test
    void deleteTask_NotFound_Returns404() throws Exception {
        String subject = createAccountSubject();
        createAccount(subject);

        mockMvc.perform(delete("/v1/tasks/{id}", 999_999L)
                .with(jwt().jwt(jwt -> jwt.subject(subject))))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_FromDifferentIdentity_Returns404() throws Exception {
        String ownerSubject = createAccountSubject();
        long ownerAccountId = createAccount(ownerSubject);

        StaticTask task = new StaticTask();
        task.setAccount(accountRepository.getReferenceById(ownerAccountId));
        task.setName("Foreign task");
        task.setDescription("Should not be deletable");
        task.setDifficulty(1);
        task.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setLabels(new ArrayList<>());
        task.setIsBlocker(false);
        long taskId = taskRepository.saveAndFlush(task).getId();

        String attackerSubject = createAccountSubject();
        createAccount(attackerSubject);

        mockMvc.perform(delete("/v1/tasks/{id}", taskId)
                .with(jwt().jwt(jwt -> jwt.subject(attackerSubject))))
            .andExpect(status().isNotFound());
    }
}