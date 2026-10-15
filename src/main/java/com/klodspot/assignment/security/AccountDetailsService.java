package com.klodspot.assignment.security;

import com.klodspot.assignment.account.AccountRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Profile("cloud")
public class AccountDetailsService implements UserDetailsService {

    private final AccountRepository accounts;
    private final Environment environment;

    public AccountDetailsService(AccountRepository accounts, Environment environment) {
        this.accounts = accounts;
        this.environment = environment;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String login = username.strip();
        return accounts
            .findByUsernameIgnoreCaseOrEmailIgnoreCase(login, login)
            .filter(account -> !account.isDemo() || environment.acceptsProfiles(Profiles.of("demo")))
            .map(AccountPrincipal::new)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid login credentials"));
    }
}
