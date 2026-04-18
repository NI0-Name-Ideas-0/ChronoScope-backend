package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.model.Account;
import jakarta.persistence.EntityManager;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class AccountProxyProvider {

    private final EntityManager entityManager;

    public AccountProxyProvider(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Named("accountProxy")
    public Account getReference(Long accountId) {
        return entityManager.getReference(Account.class, accountId);
    }
}
