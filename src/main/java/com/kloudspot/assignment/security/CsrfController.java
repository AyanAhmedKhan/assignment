package com.kloudspot.assignment.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {

    private final CurrentAccount currentAccount;
    private final Environment environment;

    public CsrfController(CurrentAccount currentAccount, Environment environment) {
        this.currentAccount = currentAccount;
        this.environment = environment;
    }

    @GetMapping("/api/session")
    public CurrentAccount.SessionInfo session() {
        return currentAccount.sessionInfo();
    }

    @GetMapping("/api/csrf")
    public CsrfResponse csrf(HttpServletRequest request) {
        var token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        boolean demoAccounts = environment.acceptsProfiles(Profiles.of("cloud & demo"));
        return token == null
            ? new CsrfResponse(false, null, null, null, false)
            : new CsrfResponse(true, token.getHeaderName(), token.getParameterName(), token.getToken(), demoAccounts);
    }

    public record CsrfResponse(
        boolean enabled,
        String headerName,
        String parameterName,
        String token,
        boolean demoAccounts
    ) {}
}
