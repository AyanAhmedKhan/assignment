package com.kloudspot.assignment.account;

import java.util.Map;

public class RegistrationException extends RuntimeException {

    private final Map<String, String> errors;

    public RegistrationException(String field, String message) {
        super(message);
        this.errors = Map.of(field, message);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
