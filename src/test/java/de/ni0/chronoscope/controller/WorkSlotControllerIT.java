package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import de.ni0.chronoscope.repository.WorkSlotRepository;
import de.ni0.chronoscope.service.KeycloakService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

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
    private WorkSlotRepository workSlotRepository;

    @Autowired
    private KeycloakService keycloakService;

    private Account createAccount(String subject) {
        Identity identity = identityRepository.saveAndFlush(new Identity());
        Account account = new Account();
        account.setIdentity(identity);
        account.setSubject(subject);
        return accountRepository.saveAndFlush(account);
    }

    private long createWorkSlot(Identity identity, String organizationId, String startAt, String endAt) {
        WorkSlot slot = new WorkSlot();
        slot.setIdentity(identity);
        slot.setOrganization(organizationId);
        slot.setStartAt(Instant.parse(startAt));
        slot.setEndAt(Instant.parse(endAt));
        return workSlotRepository.saveAndFlush(slot).getId();
    }

    @Test
    void getWorkSlots_ReturnsOwnSlotsOnly() throws Exception {
        String subject = UUID.randomUUID().toString();
        Account account = createAccount(subject);
        String orgId = UUID.randomUUID().toString();
        createWorkSlot(account.getIdentity(), orgId, "2026-04-20T08:00:00Z", "2026-04-20T17:00:00Z");

        // A slot belonging to a different identity should not appear
        Account otherAccount = createAccount(UUID.randomUUID().toString());
        createWorkSlot(otherAccount.getIdentity(), orgId, "2026-04-21T08:00:00Z", "2026-04-21T17:00:00Z");

        mockMvc.perform(get("/v1/workslots")
                        .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].identityId").value(account.getIdentity().getId()));
    }

    @Test
    void createWorkSlot_ReturnsCreated() throws Exception {
        String subject = UUID.randomUUID().toString();
        Account account = createAccount(subject);
        String orgId = UUID.randomUUID().toString();

        String payload = """
                {
                  "organizationId": %s,
                  "startAt": "2026-04-20T08:00:00Z",
                  "endAt": "2026-04-20T17:00:00Z"
                }
                """.formatted(orgId);

        mockMvc.perform(post("/v1/workslots")
                        .with(jwt().jwt(jwt -> jwt.subject(subject).claim("organization", java.util.List.of("private"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identityId").value(account.getIdentity().getId()))
                .andExpect(jsonPath("$.organizationId").value(orgId))
                .andExpect(jsonPath("$.startAt").value("2026-04-20T08:00:00Z"))
                .andExpect(jsonPath("$.endAt").value("2026-04-20T17:00:00Z"));
    }

    @Test
    void updateWorkSlot_ReturnsUpdatedSlot() throws Exception {
        String subject = UUID.randomUUID().toString();
        Account account = createAccount(subject);
        String orgId = UUID.randomUUID().toString();
        long slotId = createWorkSlot(account.getIdentity(), orgId, "2026-04-20T08:00:00Z", "2026-04-20T17:00:00Z");

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
        String ownerSubject = UUID.randomUUID().toString();
        Account ownerAccount = createAccount(ownerSubject);
        String orgId = UUID.randomUUID().toString();
        long slotId = createWorkSlot(ownerAccount.getIdentity(), orgId, "2026-04-20T08:00:00Z", "2026-04-20T17:00:00Z");

        String attackerSubject = UUID.randomUUID().toString();
        createAccount(attackerSubject);

        String payload = """
                { "startAt": "2026-04-21T09:00:00Z" }
                """;

        mockMvc.perform(patch("/v1/workslots/" + slotId)
                        .with(jwt().jwt(jwt -> jwt.subject(attackerSubject)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWorkSlot_ReturnsNoContent() throws Exception {
        String subject = UUID.randomUUID().toString();
        Account account = createAccount(subject);
        String orgId = UUID.randomUUID().toString();
        long slotId = createWorkSlot(account.getIdentity(), orgId, "2026-04-20T08:00:00Z", "2026-04-20T17:00:00Z");

        mockMvc.perform(delete("/v1/workslots/" + slotId)
                        .with(jwt().jwt(jwt -> jwt.subject(subject).claim("organization", java.util.List.of("private")))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteWorkSlot_ReturnsNotFoundWhenSlotDoesNotBelongToIdentity() throws Exception {
        String ownerSubject = UUID.randomUUID().toString();
        Account ownerAccount = createAccount(ownerSubject);
        String orgId = UUID.randomUUID().toString();
        long slotId = createWorkSlot(ownerAccount.getIdentity(), orgId, "2026-04-20T08:00:00Z", "2026-04-20T17:00:00Z");

        String attackerSubject = UUID.randomUUID().toString();
        createAccount(attackerSubject);

        mockMvc.perform(delete("/v1/workslots/" + slotId)
                        .with(jwt().jwt(jwt -> jwt.subject(attackerSubject).claim("organization", java.util.List.of("private")))))
                .andExpect(status().isNotFound());
    }
}
