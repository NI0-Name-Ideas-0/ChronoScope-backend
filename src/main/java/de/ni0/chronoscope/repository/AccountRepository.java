package de.ni0.chronoscope.repository;

import de.ni0.chronoscope.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
