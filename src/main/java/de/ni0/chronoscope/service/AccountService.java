package de.ni0.chronoscope.service;

import java.util.List;

import org.springframework.stereotype.Service;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Organization;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final OrganizationRepository organizationRepository;

    public long syncAccount(String subject, List<String> organizationNames) {
        // Find or create account by subject
        Account account = accountRepository.findBySubject(subject)
            .orElseGet(() -> {
                Account newAccount = new Account();
                newAccount.setSubject(subject);
                return accountRepository.save(newAccount);
            });

        // Sync organizations
        List<Organization> organizations = organizationNames.stream()
            .map(name -> {
                Organization org = organizationRepository.findByName(name)
                    .orElseGet(() -> {
                        Organization newOrg = new Organization();
                        newOrg.setName(name);
                        return organizationRepository.save(newOrg);
                    });
                return org;
            })
            .toList();

        account.setOrganizations(organizations);
        accountRepository.save(account);
        return account.getId();
    }

}
