package com.kloudspot.assignment.employee;

import com.kloudspot.assignment.employee.dto.EmployeeRequest;
import com.kloudspot.assignment.employee.dto.EmployeeResponse;
import com.kloudspot.assignment.employee.dto.EmployeeSummary;
import com.kloudspot.assignment.employee.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Employee records, search and directory totals")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
        summary = "List employees",
        description = "Zero-based pagination. Search matches name or email, ignoring case. Filters combine with AND."
    )
    public PageResponse<EmployeeResponse> list(
        @RequestParam(required = false) @Size(max = 120) String search,
        @RequestParam(required = false) Department department,
        @RequestParam(required = false) EmploymentStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "fullName") String sortBy,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.list(search, department, status, page, size, sortBy, direction);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get totals for the whole directory")
    public EmployeeSummary summary() {
        return service.summary();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one employee")
    public EmployeeResponse get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create an employee")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        var employee = service.create(request);
        return ResponseEntity.created(URI.create("/api/employees/" + employee.id())).body(employee);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an employee's editable fields")
    public EmployeeResponse update(@PathVariable long id, @Valid @RequestBody EmployeeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an employee permanently")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
