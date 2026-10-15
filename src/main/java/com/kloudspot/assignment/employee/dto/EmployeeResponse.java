package com.kloudspot.assignment.employee.dto;

import com.kloudspot.assignment.employee.Department;
import com.kloudspot.assignment.employee.Employee;
import com.kloudspot.assignment.employee.EmploymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeResponse(
    Long id,
    String fullName,
    String email,
    String phoneNumber,
    Department department,
    String designation,
    LocalDate dateOfJoining,
    BigDecimal salary,
    EmploymentStatus status
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
            employee.getId(),
            employee.getFullName(),
            employee.getEmail(),
            employee.getPhoneNumber(),
            employee.getDepartment(),
            employee.getDesignation(),
            employee.getDateOfJoining(),
            employee.getSalary(),
            employee.getStatus()
        );
    }
}
