package de.ni0.chronoscope;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TestData {

    public static Identity identity(long identityId) {
        Identity identity = new Identity();
        identity.setId(identityId);
        return identity;
    }

    public static Account account() {
        Identity identity = new Identity();
        Account account = new Account();
        account.setSubject(UUID.randomUUID().toString());
        account.setIdentity(identity);
        identity.setAccounts(Set.of(account));
        return account;
    }

    public static Account account(long identityId, long accountId) {
        Identity identity = identity(identityId);
        Account account = new Account();
        account.setSubject(UUID.randomUUID().toString());
        account.setId(accountId);
        account.setIdentity(identity);
        identity.setAccounts(Set.of(account));
        return account;
    }

}
