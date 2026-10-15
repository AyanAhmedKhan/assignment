package com.kloudspot.assignment.employee.dto;

import com.kloudspot.assignment.employee.Department;
import com.kloudspot.assignment.employee.EmploymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

public record EmployeeRequest(
    @NotBlank(message = "Enter a full name") @Size(max = 120, message = "Use at most 120 characters") String fullName,
    @NotBlank(message = "Enter an email address")
    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Use at most 254 characters")
    String email,
    @NotBlank(message = "Enter a phone number")
    @Size(max = 32, message = "Use at most 32 characters")
    @Pattern(
        regexp = "^(?=(?:\\D*\\d){7,15}\\D*$)\\+?[\\d ()-]+$",
        message = "Enter a phone number with 7 to 15 digits"
    )
    String phoneNumber,
    @NotNull(message = "Choose a department") Department department,
    @NotBlank(message = "Enter a designation")
    @Size(max = 120, message = "Use at most 120 characters")
    String designation,
    @NotNull(message = "Enter a joining date")
    @PastOrPresent(message = "Date of joining cannot be in the future")
    LocalDate dateOfJoining,
    @NotNull(message = "Enter a salary")
    @DecimalMin(value = "0.00", message = "Salary cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Use at most 10 whole-number digits and 2 decimal places")
    BigDecimal salary,
    @NotNull(message = "Choose an employment status") EmploymentStatus status
) {
    public EmployeeRequest {
        fullName = trim(fullName);
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        phoneNumber = trim(phoneNumber);
        designation = trim(designation);
    }

    private static String trim(String value) {
        return value == null ? null : value.strip();
    }
}
