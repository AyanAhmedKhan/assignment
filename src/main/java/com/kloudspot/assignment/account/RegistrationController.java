package com.kloudspot.assignment.account;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("cloud")
public class RegistrationController {

    private final AccountService accounts;

    public RegistrationController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegistrationRequest request) {
        accounts.register(request);
        return new RegistrationResponse("Your account is ready. Sign in to open your directory.");
    }

    public record RegistrationResponse(String message) {}
}
