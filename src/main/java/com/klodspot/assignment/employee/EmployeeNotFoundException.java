package com.klodspot.assignment.employee;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(long id) {
        super("Employee " + id + " was not found.");
    }
}
