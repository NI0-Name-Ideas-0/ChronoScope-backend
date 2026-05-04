package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.model.Account;
import jakarta.persistence.EntityManager;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * Supplies JPA account references for MapStruct mappings from ID values.
 */
@Component
public class AccountProxyProvider {

    private final EntityManager entityManager;

    public AccountProxyProvider(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Returns a lazy account reference without loading the entity immediately.
     *
     * @param accountId account ID to reference
     * @return account proxy
     */
    @Named("accountProxy")
    public Account getReference(Long accountId) {
        return entityManager.getReference(Account.class, accountId);
    }
}
