package com.klodspot.assignment.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsernameIgnoreCase(String username);
    Optional<Account> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
    boolean existsByEmailIgnoreCase(String email);
}
