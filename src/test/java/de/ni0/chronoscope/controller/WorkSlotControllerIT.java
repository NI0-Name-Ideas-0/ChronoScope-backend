package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.Organization;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import de.ni0.chronoscope.repository.OrganizationRepository;
import de.ni0.chronoscope.repository.WorkSlotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = "de.ni0.chronoscope.mapper")
@Transactional
class WorkSlotControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private WorkSlotRepository workSlotRepository;

    private String createAccountSubject() {
        return "it-ws-subject-" + System.nanoTime();
    }

    private long createAccount(String subject) {
        Identity identity = identityRepository.saveAndFlush(new Identity());
        Account account = new Account();
        account.setIdentity(identity);
        account.setSubject(subject);
        return accountRepository.saveAndFlush(account).getId();
    }

    private Organization findOrCreateOrganization(String name) {
        return organizationRepository.findByName(name).orElseGet(() -> {
            Organization org = new Organization();
            org.setName(name);
            return organizationRepository.saveAndFlush(org);
        });
    }

    private long createWorkSlot(long accountId, long organizationId, String startAt, String endAt) {
        WorkSlot slot = new WorkSlot();
        slot.setAccount(accountRepository.getReferenceById(accountId));
        slot.setOrganization(organizationRepository.getReferenceById(organizationId));
        slot.setStartAt(Instant.parse(startAt));
        slot.setEndAt(Instant.parse(endAt));
        return workSlotRepository.saveAndFlush(slot).getId();
    }

    @Test
    void getWorkSlots_ReturnsOwnSlotsOnly() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);
        long orgId = findOrCreateOrganization("it-ws-org-" + System.nanoTime()).getId();
        createWorkSlot(accountId, orgId, "2026-04-20T08:00:00Z", "2026-04-20T17:00:00Z");

        // A slot belonging to a different identity should not appear
        long otherAccountId = createAccount(createAccountSubject());
        createWorkSlot(otherAccountId, orgId, "2026-04-21T08:00:00Z", "2026-04-21T17:00:00Z");

        mockMvc.perform(get("/v1/workslots")
                        .with(jwt().jwt(jwt -> jwt.subject(subject).claim("organization", java.util.List.of("private")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountId").value(accountId));
    }

    @Test
    void createWorkSlot_ReturnsCreated() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);
        long orgId = findOrCreateOrganization("it-ws-org-" + System.nanoTime()).getId();

        String payload = """
                {
                  "accountId": %d,
                  "organizationId": %d,
                  "startAt": "2026-04-20T08:00:00Z",
                  "endAt": "2026-04-20T17:00:00Z"
                }
                """.formatted(accountId, orgId);

        mockMvc.perform(post("/v1/workslots")
                        .with(jwt().jwt(jwt -> jwt.subject(subject).claim("organization", java.util.List.of("private"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.organizationId").value(orgId))
                .andExpect(jsonPath("$.startAt").value("2026-04-20T08:00:00Z"))
                .andExpect(jsonPath("$.endAt").value("2026-04-20T17:00:00Z"));
    }

    @Test
    void createWorkSlot_ReturnsForbiddenWhenAccountBelongsToDifferentIdentity() throws Exception {
        String ownerSubject = createAccountSubject();
        long otherAccountId = createAccount(ownerSubject);
        long orgId = findOrCreateOrganization("it-ws-org-" + System.nanoTime()).getId();

        // Authenticated as a different user
        String attackerSubject = createAccountSubject();
        createAccount(attackerSubject);

        String payload = """
                {
                  "accountId": %d,
                  "organizationId": %d,
                  "startAt": "2026-04-20T08:00:00Z",
                  "endAt": "2026-04-20T17:00:00Z"
                }
                """.formatted(otherAccountId, orgId);

        mockMvc.perform(post("/v1/workslots")
                        .with(jwt().jwt(jwt -> jwt.subject(attackerSubject).claim("organization", java.util.List.of("private"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateWorkSlot_ReturnsUpdatedSlot() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);
        long orgId = findOrCreateOrganization("it-ws-org-" + System.nanoTime()).getId();
        long slotId = createWorkSlot(accountId, orgId, "2026-04-20T08:00:00Z", "2026-04-20T17:00:00Z");

        String payload = """
                {
                  "startAt": "2026-04-21T09:00:00Z",
                  "endAt": "2026-04-21T18:00:00Z"
                }
                """;

        mockMvc.perform(patch("/v1/workslots/" + slotId)
                        .with(jwt().jwt(jwt -> jwt.subject(subject).claim("organization", java.util.List.of("private"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(slotId))
                .andExpect(jsonPath("$.startAt").value("2026-04-21T09:00:00Z"))
                .andExpect(jsonPath("$.endAt").value("2026-04-21T18:00:00Z"));
    }

    @Test
    void updateWorkSlot_ReturnsNotFoundWhenSlotDoesNotBelongToIdentity() throws Exception {
        String ownerSubject = createAccountSubject();
        long ownerAccountId = createAccount(ownerSubject);
        long orgId = findOrCreateOrganization("it-ws-org-" + System.nanoTime()).getId();
        long slotId = createWorkSlot(ownerAccountId, orgId, "2026-04-20T08:00:00Z", "2026-04-20T17:00:00Z");

        String attackerSubject = createAccountSubject();
        createAccount(attackerSubject);

        String payload = """
                { "startAt": "2026-04-21T09:00:00Z" }
                """;

        mockMvc.perform(patch("/v1/workslots/" + slotId)
                        .with(jwt().jwt(jwt -> jwt.subject(attackerSubject).claim("organization", java.util.List.of("private"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWorkSlot_ReturnsNoContent() throws Exception {
        String subject = createAccountSubject();
        long accountId = createAccount(subject);
        long orgId = findOrCreateOrganization("it-ws-org-" + System.nanoTime()).getId();
        long slotId = createWorkSlot(accountId, orgId, "2026-04-20T08:00:00Z", "2026-04-20T17:00:00Z");

        mockMvc.perform(delete("/v1/workslots/" + slotId)
                        .with(jwt().jwt(jwt -> jwt.subject(subject).claim("organization", java.util.List.of("private")))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteWorkSlot_ReturnsNotFoundWhenSlotDoesNotBelongToIdentity() throws Exception {
        String ownerSubject = createAccountSubject();
        long ownerAccountId = createAccount(ownerSubject);
        long orgId = findOrCreateOrganization("it-ws-org-" + System.nanoTime()).getId();
        long slotId = createWorkSlot(ownerAccountId, orgId, "2026-04-20T08:00:00Z", "2026-04-20T17:00:00Z");

        String attackerSubject = createAccountSubject();
        createAccount(attackerSubject);

        mockMvc.perform(delete("/v1/workslots/" + slotId)
                        .with(jwt().jwt(jwt -> jwt.subject(attackerSubject).claim("organization", java.util.List.of("private")))))
                .andExpect(status().isNotFound());
    }
}
