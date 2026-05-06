package de.ni0.chronoscope.repository;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.Label;
import de.ni0.chronoscope.model.Scope;

@SpringBootTest
@Transactional
class RepositoryMappingsIT {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Test
    void accountRepository_FindsAccountBySubject() {
        Identity identity = identityRepository.saveAndFlush(new Identity());

        Account account = new Account();
        account.setSubject("subject-123");
        account.setIdentity(identity);
        accountRepository.saveAndFlush(account);

        assertTrue(accountRepository.findBySubject("subject-123").isPresent());
        assertEquals(account.getId(), accountRepository.findBySubject("subject-123").orElseThrow().getId());
    }

    @Test
    void identityRepository_FindsIdentityContainingAccount() {
        Identity identity = identityRepository.saveAndFlush(new Identity());

        Account account = new Account();
        account.setSubject("subject-456");
        account.setIdentity(identity);
        account = accountRepository.saveAndFlush(account);

        assertTrue(identityRepository.findByAccountsContains(account).isPresent());
        assertEquals(identity.getId(), identityRepository.findByAccountsContains(account).orElseThrow().getId());
    }

    @Test
    void labelRepository_PersistsLabelName() {
        Label label = new Label();
        label.setName("urgent");
        labelRepository.saveAndFlush(label);

        assertEquals(label.getId(), labelRepository.findById(label.getId()).orElseThrow().getId());
        assertEquals("urgent", labelRepository.findById(label.getId()).orElseThrow().getName());
    }

    @Test
    void taskRepository_PersistsDynamicTaskDependenciesViaJoinTable() {
        Identity identity = identityRepository.saveAndFlush(new Identity());
        String organization = UUID.randomUUID().toString();

        Account account = new Account();
        account.setSubject("subject-dependency-test");
        account.setIdentity(identity);
        account = accountRepository.saveAndFlush(account);

        DynamicTask predecessor = new DynamicTask();
        predecessor.setIdentity(identity);
        predecessor.setOrganizationId(organization);
        predecessor.setName("Predecessor");
        predecessor.setDescription("Dependency source");
        predecessor.setDifficulty(1);
        predecessor.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        predecessor.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        predecessor.setDuration(Duration.of(60, ChronoUnit.MINUTES));
        predecessor.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        predecessor.setMinScopeDuration(Duration.of(15, ChronoUnit.MINUTES));
        predecessor.setMaxScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        predecessor.setLabels(new ArrayList<>());
        predecessor.setScopes(new ArrayList<>());
        predecessor.setDependencies(new HashSet<>());
        predecessor.setDependents(new HashSet<>());
        predecessor = taskRepository.saveAndFlush(predecessor);

        DynamicTask dependent = new DynamicTask();
        dependent.setIdentity(identity);
        dependent.setOrganizationId(organization);
        dependent.setName("Dependent");
        dependent.setDescription("Depends on predecessor");
        dependent.setDifficulty(2);
        dependent.setStartAt(Instant.parse("2026-04-20T10:00:00Z"));
        dependent.setEndAt(Instant.parse("2026-04-20T12:00:00Z"));
        dependent.setDuration(Duration.of(120, ChronoUnit.MINUTES));
        dependent.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        dependent.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        dependent.setMaxScopeDuration(Duration.of(60, ChronoUnit.MINUTES));
        dependent.setLabels(new ArrayList<>());
        dependent.setScopes(new ArrayList<>());
        dependent.setDependencies(new HashSet<>(Set.of(predecessor)));
        dependent.setDependents(new HashSet<>());
        dependent = taskRepository.saveAndFlush(dependent);

        DynamicTask reloadedDependent = (DynamicTask) taskRepository.findById(dependent.getId()).orElseThrow();

        assertEquals(1, reloadedDependent.getDependencies().size());
        assertEquals(predecessor.getId(), reloadedDependent.getDependencies().iterator().next().getId());
    }

    @Test
    void scopeRepository_DeleteByDynamicTaskIdIn_DeletesOnlyMatchingScopes() {
        Identity identity = identityRepository.saveAndFlush(new Identity());
        String organization = UUID.randomUUID().toString();

        DynamicTask taskToDelete = buildDynamicTask(identity, organization);
        DynamicTask taskToKeep = buildDynamicTask(identity, organization);

        Scope scopeToDelete = new Scope(null, taskToDelete,
                Instant.parse("2026-04-26T08:00:00Z"), Instant.parse("2026-04-26T09:00:00Z"));
        Scope scopeToKeep = new Scope(null, taskToKeep,
                Instant.parse("2026-04-26T09:00:00Z"), Instant.parse("2026-04-26T10:00:00Z"));
        scopeRepository.saveAndFlush(scopeToDelete);
        scopeRepository.saveAndFlush(scopeToKeep);

        long deleted = scopeRepository.deleteByDynamicTaskIdIn(List.of(taskToDelete.getId()));

        assertEquals(1L, deleted);
        assertTrue(scopeRepository.findById(scopeToKeep.getId()).isPresent());
    }

    private DynamicTask buildDynamicTask(Identity identity, String organization) {
        DynamicTask task = new DynamicTask();
        task.setIdentity(identity);
        task.setOrganizationId(organization);
        task.setName("repo-it-task-" + System.nanoTime());
        task.setDescription("Test task");
        task.setDifficulty(1);
        task.setStartAt(Instant.parse("2026-04-26T08:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-26T18:00:00Z"));
        task.setDuration(Duration.of(60, ChronoUnit.MINUTES));
        task.setElapsed(Duration.of(0, ChronoUnit.MINUTES));
        task.setMinScopeDuration(Duration.of(30, ChronoUnit.MINUTES));
        task.setMaxScopeDuration(Duration.of(60, ChronoUnit.MINUTES));
        task.setLabels(new ArrayList<>());
        task.setScopes(new ArrayList<>());
        task.setDependencies(new HashSet<>());
        task.setDependents(new HashSet<>());
        return taskRepository.saveAndFlush(task);
    }
}
