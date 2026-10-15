package com.kloudspot.assignment.employee;

import com.kloudspot.assignment.employee.dto.EmployeeRequest;
import com.kloudspot.assignment.employee.dto.EmployeeResponse;
import com.kloudspot.assignment.employee.dto.EmployeeSummary;
import com.kloudspot.assignment.employee.dto.PageResponse;
import com.kloudspot.assignment.security.CurrentAccount;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private static final Set<String> SORT_FIELDS = Set.of("fullName", "dateOfJoining", "id");
    private final EmployeeRepository repository;
    private final CurrentAccount currentAccount;

    public EmployeeService(EmployeeRepository repository, CurrentAccount currentAccount) {
        this.repository = repository;
        this.currentAccount = currentAccount;
    }

    public PageResponse<EmployeeResponse> list(
        String search,
        Department department,
        EmploymentStatus status,
        int page,
        int size,
        String sortBy,
        String direction
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be 0 or greater; size must be between 1 and 100.");
        }
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("The requested page is beyond the supported range.");
        }
        if (!SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported sort field.");
        }
        Sort.Direction order;
        try {
            order = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Direction must be asc or desc.");
        }
        // A unique tie-breaker keeps employees from moving between pages with equal names or dates.
        Sort sort = Sort.by(order, sortBy).and(Sort.by("id"));
        var filter = EmployeeSpecifications.ownedBy(currentAccount.ownerId()).and(
            EmployeeSpecifications.matching(search, department, status)
        );
        var employees = repository.findAll(filter, PageRequest.of(page, size, sort));
        return PageResponse.from(employees.map(EmployeeResponse::from));
    }

    public EmployeeResponse get(long id) {
        return EmployeeResponse.from(findEmployee(id));
    }

    public EmployeeSummary summary() {
        Long ownerId = currentAccount.ownerId();
        return new EmployeeSummary(
            repository.countByOwnerId(ownerId),
            repository.countByStatusAndOwnerId(EmploymentStatus.ACTIVE, ownerId),
            repository.countByStatusAndOwnerId(EmploymentStatus.INACTIVE, ownerId),
            repository.countDepartments(ownerId)
        );
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Long ownerId = currentAccount.ownerId();
        if (repository.existsByEmailIgnoreCaseAndOwnerId(request.email(), ownerId)) {
            throw new DuplicateEmailException();
        }
        return EmployeeResponse.from(repository.saveAndFlush(new Employee(request, ownerId)));
    }

    @Transactional
    public EmployeeResponse update(long id, EmployeeRequest request) {
        Employee employee = findEmployee(id);
        if (repository.existsByEmailIgnoreCaseAndIdNotAndOwnerId(request.email(), id, currentAccount.ownerId())) {
            throw new DuplicateEmailException();
        }
        employee.update(request);
        return EmployeeResponse.from(repository.saveAndFlush(employee));
    }

    @Transactional
    public void delete(long id) {
        repository.delete(findEmployee(id));
        repository.flush();
    }

    private Employee findEmployee(long id) {
        return repository
            .findByIdAndOwnerId(id, currentAccount.ownerId())
            .orElseThrow(() -> new EmployeeNotFoundException(id));
    }
}
