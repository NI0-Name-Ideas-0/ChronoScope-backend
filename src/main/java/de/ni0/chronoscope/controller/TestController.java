package de.ni0.chronoscope.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Label;
import de.ni0.chronoscope.model.Organization;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.OrganizationRepository;
import de.ni0.chronoscope.repository.TaskRepository;
import de.ni0.chronoscope.repository.WorkSlotRepository;
import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.TaskService;
import de.ni0.chronoscope.service.WorkSlotService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/test")
@Profile("dev & !prod")
@RequiredArgsConstructor
public class TestController {

    private static final String SEED_SUBJECT = "local-test-user";
    private static final List<String> SEED_ORGANIZATIONS = List.of("private", "chronoscope-local");

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final OrganizationRepository organizationRepository;
    private final TaskRepository taskRepository;
    private final WorkSlotRepository workSlotRepository;
    private final TaskService taskService;
    private final WorkSlotService workSlotService;

    @GetMapping
    public String testEndpoint() {
        return "Hello from ChronoScope! Test Test";
    }

    /**
     * This endpoint is intended for local testing and development only. <br>
     * VIBECODE WARNING! Correctness and idempotency are not guaranteed. Use at your own risk.
     */
    @PostMapping("/seed")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SeedDataResponse seedDatabase() {
        Account account = syncSeedAccount();
        clearSeedData(account);

        List<Organization> organizations = SEED_ORGANIZATIONS.stream()
            .map(this::getOrganization)
            .toList();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate startDate = LocalDate.now(zone).plusDays(1);

        List<Task> tasks = createTasks(account, startDate, zone);
        List<WorkSlot> workSlots = createWorkSlots(account, organizations, startDate, zone);

        return new SeedDataResponse(
            SEED_SUBJECT,
            account.getIdentity().getId(),
            account.getId(),
            organizations.stream().map(Organization::getId).toList(),
            tasks.stream().map(Task::getId).toList(),
            workSlots.stream().map(WorkSlot::getId).toList()
        );
    }

    private Account syncSeedAccount() {
        accountService.syncAccount(SEED_SUBJECT, SEED_ORGANIZATIONS);
        return accountRepository.findBySubject(SEED_SUBJECT)
            .orElseThrow(() -> new IllegalStateException("Seed account was not created"));
    }

    private Organization getOrganization(String name) {
        return organizationRepository.findByName(name)
            .orElseThrow(() -> new IllegalStateException("Seed organization was not created: " + name));
    }

    private void clearSeedData(Account account) {
        long identityId = account.getIdentity().getId();

        List<WorkSlot> workSlots = workSlotRepository.findByAccountIdentityId(identityId);
        workSlotRepository.deleteAll(workSlots);
        workSlotRepository.flush();

        List<Task> tasks = List.copyOf(taskRepository.findByAccountIdentityId(identityId));
        for (Task task : tasks) {
            taskService.deleteTask(identityId, task.getId());
        }
    }

    private List<Task> createTasks(Account account, LocalDate startDate, ZoneId zone) {
        List<Task> tasks = new ArrayList<>();

        tasks.add(taskService.createStaticTask(staticTask(
            account,
            "Daily standup",
            "Recurring team check-in for local development data.",
            1,
            at(startDate, 9, 0, zone),
            at(startDate, 9, 30, zone),
            "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
            false,
            "meeting",
            "team"
        )));

        tasks.add(taskService.createStaticTask(staticTask(
            account,
            "Architecture review",
            "A one-off blocker for validating the scheduling flow.",
            3,
            at(startDate.plusDays(2), 14, 0, zone),
            at(startDate.plusDays(2), 15, 30, zone),
            "FREQ=DAILY;COUNT=1",
            true,
            "review",
            "blocker"
        )));

        DynamicTask planningView = taskService.createDynamicTask(dynamicTask(
            account,
            "Implement planning view",
            "Build the first pass of the planning timeline.",
            4,
            at(startDate, 8, 0, zone),
            at(startDate.plusDays(14), 18, 0, zone),
            "FREQ=DAILY;INTERVAL=1",
            Duration.ofHours(8),
            Duration.ofHours(1),
            Duration.ofMinutes(45),
            Duration.ofHours(3),
            List.of(
                new ScopeWindow(at(startDate, 10, 0, zone), at(startDate.plusDays(5), 17, 0, zone)),
                new ScopeWindow(at(startDate.plusDays(6), 9, 0, zone), at(startDate.plusDays(14), 18, 0, zone))
            ),
            "frontend",
            "planning"
        ));
        tasks.add(planningView);

        DynamicTask releaseNotes = dynamicTask(
            account,
            "Write release notes",
            "Summarize the local seed workflow and planning changes.",
            2,
            at(startDate, 8, 0, zone),
            at(startDate.plusDays(14), 18, 0, zone),
            "FREQ=DAILY;INTERVAL=1",
            Duration.ofHours(2),
            Duration.ZERO,
            Duration.ofMinutes(30),
            Duration.ofHours(2),
            List.of(new ScopeWindow(at(startDate.plusDays(3), 9, 0, zone), at(startDate.plusDays(14), 18, 0, zone))),
            "docs",
            "release"
        );
        releaseNotes.setDependencies(new HashSet<>(Set.of(planningView)));
        tasks.add(taskService.createDynamicTask(releaseNotes));

        return tasks;
    }

    private StaticTask staticTask(
        Account account,
        String name,
        String description,
        int difficulty,
        Instant startAt,
        Instant endAt,
        String rrule,
        boolean isBlocker,
        String... labels
    ) {
        StaticTask task = new StaticTask();
        applyTaskFields(task, account, name, description, difficulty, startAt, endAt, rrule, labels);
        task.setIsBlocker(isBlocker);
        return task;
    }

    private DynamicTask dynamicTask(
        Account account,
        String name,
        String description,
        int difficulty,
        Instant startAt,
        Instant endAt,
        String rrule,
        Duration duration,
        Duration elapsed,
        Duration minScopeDuration,
        Duration maxScopeDuration,
        List<ScopeWindow> scopeWindows,
        String... labels
    ) {
        DynamicTask task = new DynamicTask();
        applyTaskFields(task, account, name, description, difficulty, startAt, endAt, rrule, labels);
        task.setDuration(duration);
        task.setElapsed(elapsed);
        task.setMinScopeDuration(minScopeDuration);
        task.setMaxScopeDuration(maxScopeDuration);
        task.setDependencies(new HashSet<>());
        task.setDependents(new HashSet<>());
        task.setScopes(scopesFor(task, scopeWindows));
        return task;
    }

    private void applyTaskFields(
        Task task,
        Account account,
        String name,
        String description,
        int difficulty,
        Instant startAt,
        Instant endAt,
        String rrule,
        String... labels
    ) {
        task.setAccount(account);
        task.setName(name);
        task.setDescription(description);
        task.setDifficulty(difficulty);
        task.setStartAt(startAt);
        task.setEndAt(endAt);
        task.setRrule(rrule);
        task.setLabels(labelsFor(task, labels));
    }

    private List<Label> labelsFor(Task task, String... names) {
        List<Label> labels = new ArrayList<>();
        for (String name : names) {
            Label label = new Label();
            label.setTask(task);
            label.setName(name);
            labels.add(label);
        }
        return labels;
    }

    private List<Scope> scopesFor(DynamicTask task, List<ScopeWindow> windows) {
        List<Scope> scopes = new ArrayList<>();
        for (ScopeWindow window : windows) {
            Scope scope = new Scope();
            scope.setDynamicTask(task);
            scope.setStartAt(window.startAt());
            scope.setEndAt(window.endAt());
            scopes.add(scope);
        }
        return scopes;
    }

    private List<WorkSlot> createWorkSlots(Account account, List<Organization> organizations, LocalDate startDate, ZoneId zone) {
        Organization privateOrganization = organizations.get(0);
        Organization localOrganization = organizations.get(1);

        return List.of(
            workSlotService.createWorkSlot(workSlot(
                account,
                privateOrganization,
                at(startDate, 10, 0, zone),
                at(startDate, 12, 0, zone)
            )),
            workSlotService.createWorkSlot(workSlot(
                account,
                localOrganization,
                at(startDate, 13, 0, zone),
                at(startDate, 16, 0, zone)
            )),
            workSlotService.createWorkSlot(workSlot(
                account,
                localOrganization,
                at(startDate.plusDays(1), 9, 0, zone),
                at(startDate.plusDays(1), 12, 30, zone)
            )),
            workSlotService.createWorkSlot(workSlot(
                account,
                privateOrganization,
                at(startDate.plusDays(2), 14, 0, zone),
                at(startDate.plusDays(2), 17, 0, zone)
            ))
        );
    }

    private WorkSlot workSlot(Account account, Organization organization, Instant startAt, Instant endAt) {
        WorkSlot workSlot = new WorkSlot();
        workSlot.setAccount(account);
        workSlot.setOrganization(organization);
        workSlot.setStartAt(startAt);
        workSlot.setEndAt(endAt);
        return workSlot;
    }

    private Instant at(LocalDate date, int hour, int minute, ZoneId zone) {
        return date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant();
    }

    public record SeedDataResponse(
        String subject,
        Long identityId,
        Long accountId,
        List<Long> organizationIds,
        List<Long> taskIds,
        List<Long> workSlotIds
    ) {
    }

    private record ScopeWindow(Instant startAt, Instant endAt) {
    }
}
