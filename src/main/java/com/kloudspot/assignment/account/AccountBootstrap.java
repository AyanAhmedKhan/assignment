package com.kloudspot.assignment.account;

import com.kloudspot.assignment.employee.EmployeeRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Component
@Profile("cloud")
@Order(10)
public class AccountBootstrap implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "KlodspotDemo2026!";

    private final AccountRepository accounts;
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;
    private final Environment environment;
    private final String adminUsername;
    private final String adminPassword;

    public AccountBootstrap(
        AccountRepository accounts,
        EmployeeRepository employees,
        PasswordEncoder encoder,
        Environment environment,
        @Value("${app.admin.username}") String adminUsername,
        @Value("${app.admin.password}") String adminPassword
    ) {
        this.accounts = accounts;
        this.employees = employees;
        this.encoder = encoder;
        this.environment = environment;
        this.adminUsername = adminUsername.strip().toLowerCase(Locale.ROOT);
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Assert.hasText(adminUsername, "ADMIN_USERNAME must not be blank");
        Assert.isTrue(
            adminPassword.length() >= 16 && adminPassword.getBytes(StandardCharsets.UTF_8).length <= 72,
            "ADMIN_PASSWORD must have at least 16 characters and at most 72 UTF-8 bytes"
        );
        Account admin = accounts.findByUsernameIgnoreCase(adminUsername).orElse(null);
        if (admin == null) {
            admin = accounts.saveAndFlush(
                new Account(
                    adminUsername,
                    "admin@klodspot.invalid",
                    "Administrator",
                    encoder.encode(adminPassword),
                    AccountRole.ADMIN,
                    false
                )
            );
            employees.assignUnownedEmployees(admin.getId());
        } else {
            Assert.isTrue(admin.getRole() == AccountRole.ADMIN, "ADMIN_USERNAME is already used by another account");
            if (!encoder.matches(adminPassword, admin.getPasswordHash())) {
                admin.changePasswordHash(encoder.encode(adminPassword));
            }
        }

        if (environment.acceptsProfiles(Profiles.of("demo"))) {
            createDemoAccount("demo", "Demo editor", AccountRole.MEMBER);
            createDemoAccount("viewer", "Demo viewer", AccountRole.VIEWER);
        }
    }

    private void createDemoAccount(String username, String displayName, AccountRole role) {
        if (accounts.findByUsernameIgnoreCase(username).isEmpty()) {
            accounts.save(
                new Account(
                    username,
                    username + "@klodspot.invalid",
                    displayName,
                    encoder.encode(DEMO_PASSWORD),
                    role,
                    true
                )
            );
        }
    }
}
