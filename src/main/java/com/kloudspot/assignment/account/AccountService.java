package com.kloudspot.assignment.account;

import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegistrationRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new RegistrationException("confirmPassword", "The passwords do not match.");
        }
        // BCrypt accepts at most 72 UTF-8 bytes, which can be fewer than 64 characters.
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegistrationException(
                "password",
                "This password is too long when encoded. Use fewer characters."
            );
        }
        if (accounts.existsByEmailIgnoreCase(request.email())) {
            throw new RegistrationException("email", "An account with this email already exists.");
        }

        Account account = new Account(
            request.email(),
            request.email(),
            request.fullName(),
            passwordEncoder.encode(request.password()),
            AccountRole.MEMBER,
            false
        );
        accounts.saveAndFlush(account);
    }
}
