package com.kloudspot.assignment.security;

import com.kloudspot.assignment.account.Account;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class AccountPrincipal extends User {

    private final Long accountId;
    private final String displayName;
    private final String role;
    private final boolean demo;

    public AccountPrincipal(Account account) {
        super(
            account.getUsername(),
            account.getPasswordHash(),
            List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()))
        );
        this.accountId = account.getId();
        this.displayName = account.getDisplayName();
        this.role = account.getRole().name();
        this.demo = account.isDemo();
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public boolean isDemo() {
        return demo;
    }
}
