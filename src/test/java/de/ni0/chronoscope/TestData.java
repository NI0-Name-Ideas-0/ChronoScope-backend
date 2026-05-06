package de.ni0.chronoscope;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;

import java.util.UUID;

public class TestData {
    public static long ACCOUNT_ID = 22L;
    public static long IDENTITY_ID = 44L;

    public static Identity identity() {
        return identity(IDENTITY_ID);
    }
    public static Identity identity(long identityId) {
        Identity identity = new Identity();
        identity.setId(identityId);
        return identity;
    }

    public static Account account() {
        return account(IDENTITY_ID, ACCOUNT_ID);
    }

    public static Account account(long identityId, long accountId) {
        Identity identity = identity(identityId);
        Account account = new Account();
        account.setSubject(UUID.randomUUID().toString());
        account.setId(accountId);
        account.setIdentity(identity);
        identity.getAccounts().add(account);
        return account;
    }

}
