package com.kloudspot.assignment.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record RegistrationRequest(
    @NotBlank(message = "Enter your name") @Size(max = 120, message = "Use at most 120 characters") String fullName,

    @NotBlank(message = "Enter your email address")
    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Use at most 254 characters")
    String email,

    @NotBlank(message = "Enter a password")
    @Size(min = 12, max = 64, message = "Use a password of 12 to 64 characters")
    String password,

    @NotBlank(message = "Confirm your password")
    @Size(max = 64, message = "Use at most 64 characters")
    String confirmPassword
) {
    public RegistrationRequest {
        fullName = fullName == null ? null : fullName.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "RegistrationRequest[credentials redacted]";
    }
}
