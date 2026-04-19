package de.ni0.chronoscope.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import de.ni0.chronoscope.repository.TaskRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = "de.ni0.chronoscope.mapper")
class TaskControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TaskRepository taskRepository;

    private long createAccount() {
        return createAccount("it-subject-" + System.nanoTime());
    }

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
        predecessor.setDuration(120);
        predecessor.setElapsed(0);
        predecessor.setMinScopeDuration(30);
        predecessor.setMaxScopeDuration(60);
        predecessor.setLabels(new ArrayList<>());
        predecessor.setScopes(new ArrayList<>());
        predecessor.setDependencies(new ArrayList<>());
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
        ownTask.setDuration(120);
        ownTask.setElapsed(0);
        ownTask.setMinScopeDuration(30);
        ownTask.setMaxScopeDuration(60);
        ownTask.setLabels(new ArrayList<>());
        ownTask.setScopes(new ArrayList<>());
        ownTask.setDependencies(new ArrayList<>());
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
              "duration": 240,
              "minScopeDuration": 30,
              "maxScopeDuration": 120,
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
            .andExpect(jsonPath("$.duration").value(240))
            .andExpect(jsonPath("$.elapsed").value(0))
            .andExpect(jsonPath("$.minScopeDuration").value(30))
            .andExpect(jsonPath("$.maxScopeDuration").value(120))
            .andExpect(jsonPath("$.scopes").isArray())
            .andExpect(jsonPath("$.scopes").isEmpty())
            .andExpect(jsonPath("$.dependencies").isArray())
            .andExpect(jsonPath("$.dependencies").isEmpty());
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
              "duration": 240,
              "minScopeDuration": 30,
              "maxScopeDuration": 120,
              "dependencies": [
                {
                  "predecessorDynamicTaskId": %d
                }
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
            .andExpect(jsonPath("$.dependencies[0].dynamicTaskId").isNumber())
            .andExpect(jsonPath("$.dependencies[0].predecessorDynamicTaskId").value(predecessorId));
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
              "duration": 240,
              "minScopeDuration": 30,
              "maxScopeDuration": 120
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
}