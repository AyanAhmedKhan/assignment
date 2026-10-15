package com.klodspot.assignment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentAccount {

    private final boolean securityEnabled;

    public CurrentAccount(@Value("${app.security.enabled:false}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public Long ownerId() {
        return securityEnabled ? principal().getAccountId() : null;
    }

    public SessionInfo sessionInfo() {
        if (!securityEnabled) {
            return new SessionInfo(false, "Local workspace", "ADMIN", false);
        }
        AccountPrincipal user = principal();
        return new SessionInfo(true, user.getDisplayName(), user.getRole(), user.isDemo());
    }

    private AccountPrincipal principal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal user) {
            return user;
        }
        throw new AccessDeniedException("Sign in to access your directory.");
    }

    public record SessionInfo(boolean enabled, String displayName, String role, boolean demo) {}
}
