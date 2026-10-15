package com.kloudspot.assignment.config;

import com.kloudspot.assignment.account.AccountRepository;
import com.kloudspot.assignment.employee.Department;
import com.kloudspot.assignment.employee.Employee;
import com.kloudspot.assignment.employee.EmployeeRepository;
import com.kloudspot.assignment.employee.EmploymentStatus;
import com.kloudspot.assignment.employee.dto.EmployeeRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
@Profile("demo")
public class DemoDataConfig {

    @Bean
    @Order(20)
    ApplicationRunner loadDemoEmployees(
        EmployeeRepository repository,
        AccountRepository accounts,
        Environment environment
    ) {
        return args -> {
            if (environment.acceptsProfiles(Profiles.of("cloud"))) {
                for (String username : List.of("demo", "viewer")) {
                    Long ownerId = accounts.findByUsernameIgnoreCase(username).orElseThrow().getId();
                    seedDirectory(repository, ownerId);
                }
            } else {
                seedDirectory(repository, null);
            }
        };
    }

    private void seedDirectory(EmployeeRepository repository, Long ownerId) {
        if (repository.countByOwnerId(ownerId) > 0) {
            return;
        }
        repository.saveAll(
            List.of(
                employee(
                    ownerId,
                    "Aditi Sharma",
                    "aditi.sharma@example.com",
                    "+91 90000 10001",
                    Department.ENGINEERING,
                    "Backend Engineer",
                    "2024-06-10",
                    "850000",
                    EmploymentStatus.ACTIVE
                ),
                employee(
                    ownerId,
                    "Arjun Mehta",
                    "arjun.mehta@example.com",
                    "+91 90000 10002",
                    Department.PRODUCT,
                    "Product Manager",
                    "2023-09-04",
                    "1200000",
                    EmploymentStatus.ACTIVE
                ),
                employee(
                    ownerId,
                    "Diya Nair",
                    "diya.nair@example.com",
                    "+91 90000 10003",
                    Department.DESIGN,
                    "Product Designer",
                    "2024-02-19",
                    "780000",
                    EmploymentStatus.ACTIVE
                ),
                employee(
                    ownerId,
                    "Ishaan Kapoor",
                    "ishaan.kapoor@example.com",
                    "+91 90000 10004",
                    Department.ENGINEERING,
                    "Frontend Engineer",
                    "2024-07-01",
                    "820000",
                    EmploymentStatus.ACTIVE
                ),
                employee(
                    ownerId,
                    "Kavya Iyer",
                    "kavya.iyer@example.com",
                    "+91 90000 10005",
                    Department.HUMAN_RESOURCES,
                    "People Partner",
                    "2023-11-13",
                    "720000",
                    EmploymentStatus.ACTIVE
                ),
                employee(
                    ownerId,
                    "Meera Joshi",
                    "meera.joshi@example.com",
                    "+91 90000 10006",
                    Department.FINANCE,
                    "Financial Analyst",
                    "2023-05-22",
                    "680000",
                    EmploymentStatus.ACTIVE
                ),
                employee(
                    ownerId,
                    "Neel Desai",
                    "neel.desai@example.com",
                    "+91 90000 10007",
                    Department.OPERATIONS,
                    "Operations Associate",
                    "2024-01-08",
                    "540000",
                    EmploymentStatus.INACTIVE
                ),
                employee(
                    ownerId,
                    "Priya Menon",
                    "priya.menon@example.com",
                    "+91 90000 10008",
                    Department.ENGINEERING,
                    "QA Engineer",
                    "2024-03-11",
                    "650000",
                    EmploymentStatus.ACTIVE
                ),
                employee(
                    ownerId,
                    "Rahul Verma",
                    "rahul.verma@example.com",
                    "+91 90000 10009",
                    Department.PRODUCT,
                    "Product Analyst",
                    "2023-08-07",
                    "700000",
                    EmploymentStatus.ACTIVE
                ),
                employee(
                    ownerId,
                    "Riya Sen",
                    "riya.sen@example.com",
                    "+91 90000 10010",
                    Department.DESIGN,
                    "Visual Designer",
                    "2023-12-04",
                    "610000",
                    EmploymentStatus.INACTIVE
                ),
                employee(
                    ownerId,
                    "Rohan Patel",
                    "rohan.patel@example.com",
                    "+91 90000 10011",
                    Department.ENGINEERING,
                    "Software Engineer",
                    "2024-05-06",
                    "760000",
                    EmploymentStatus.ACTIVE
                ),
                employee(
                    ownerId,
                    "Sana Khan",
                    "sana.khan@example.com",
                    "+91 90000 10012",
                    Department.OPERATIONS,
                    "Operations Lead",
                    "2023-04-17",
                    "890000",
                    EmploymentStatus.ACTIVE
                ),
                employee(
                    ownerId,
                    "Tanvi Rao",
                    "tanvi.rao@example.com",
                    "+91 90000 10013",
                    Department.HUMAN_RESOURCES,
                    "Recruiter",
                    "2024-04-15",
                    "590000",
                    EmploymentStatus.ACTIVE
                ),
                employee(
                    ownerId,
                    "Vikram Shah",
                    "vikram.shah@example.com",
                    "+91 90000 10014",
                    Department.FINANCE,
                    "Finance Associate",
                    "2023-07-03",
                    "580000",
                    EmploymentStatus.INACTIVE
                )
            )
        );
    }

    private Employee employee(
        Long ownerId,
        String name,
        String email,
        String phone,
        Department department,
        String designation,
        String joined,
        String salary,
        EmploymentStatus status
    ) {
        return new Employee(
            new EmployeeRequest(
                name,
                email,
                phone,
                department,
                designation,
                LocalDate.parse(joined),
                new BigDecimal(salary),
                status
            ),
            ownerId
        );
    }
}
