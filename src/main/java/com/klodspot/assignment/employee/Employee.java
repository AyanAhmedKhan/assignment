package com.klodspot.assignment.employee;

import com.klodspot.assignment.employee.dto.EmployeeRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 32)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Department department;

    @Column(nullable = false, length = 120)
    private String designation;

    @Column(nullable = false)
    private LocalDate dateOfJoining;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EmploymentStatus status;

    @Version
    private long version;

    protected Employee() {}

    public Employee(EmployeeRequest request) {
        this(request, null);
    }

    public Employee(EmployeeRequest request, Long ownerId) {
        this.ownerId = ownerId;
        update(request);
    }

    public void update(EmployeeRequest request) {
        fullName = request.fullName();
        email = request.email();
        phoneNumber = request.phoneNumber();
        department = request.department();
        designation = request.designation();
        dateOfJoining = request.dateOfJoining();
        salary = request.salary();
        status = request.status();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Department getDepartment() {
        return department;
    }

    public String getDesignation() {
        return designation;
    }

    public LocalDate getDateOfJoining() {
        return dateOfJoining;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public EmploymentStatus getStatus() {
        return status;
    }
}
