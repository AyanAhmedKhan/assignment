package com.klodspot.assignment.employee;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("An employee with this email address already exists.");
    }
}
