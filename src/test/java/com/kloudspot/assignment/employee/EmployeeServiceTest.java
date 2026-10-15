package com.kloudspot.assignment.employee;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.kloudspot.assignment.employee.EmployeeNotFoundException;
import com.kloudspot.assignment.employee.dto.EmployeeRequest;
import com.kloudspot.assignment.employee.dto.EmployeeSummary;
import com.kloudspot.assignment.security.CurrentAccount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    EmployeeRepository repository;

    @Spy
    CurrentAccount currentAccount = new CurrentAccount(false);

    @InjectMocks
    EmployeeService service;

    private EmployeeRequest request;
    private Employee employee;

    @BeforeEach
    void setUp() {
        request = new EmployeeRequest(
            " Aditi Sharma ",
            " ADITI@example.com ",
            "+91 98765 43210",
            Department.ENGINEERING,
            " Backend Engineer ",
            LocalDate.of(2024, 6, 10),
            new BigDecimal("650000.50"),
            EmploymentStatus.ACTIVE
        );
        employee = new Employee(request);
        ReflectionTestUtils.setField(employee, "id", 1L);
    }

    @Test
    void createsEmployeeAndReturnsAssignedId() {
        when(repository.saveAndFlush(any(Employee.class))).thenReturn(employee);
        var response = service.create(request);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("aditi@example.com");
        assertThat(response.fullName()).isEqualTo("Aditi Sharma");
        assertThat(response.salary()).isEqualByComparingTo("650000.50");
        verify(repository).existsByEmailIgnoreCaseAndOwnerId("aditi@example.com", null);
    }

    @Test
    void rejectsDuplicateEmailBeforeWriting() {
        when(repository.existsByEmailIgnoreCaseAndOwnerId(request.email(), null)).thenReturn(true);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(DuplicateEmailException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void getsExistingEmployee() {
        when(repository.findByIdAndOwnerId(1L, null)).thenReturn(Optional.of(employee));
        assertThat(service.get(1).fullName()).isEqualTo("Aditi Sharma");
    }

    @Test
    void missingEmployeeReturnsNotFound() {
        assertThatThrownBy(() -> service.get(99)).isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void updatesEmployeeAndAllowsTheirOwnEmail() {
        var changes = new EmployeeRequest(
            "Aditi Sharma",
            request.email(),
            request.phoneNumber(),
            Department.PRODUCT,
            "Product Engineer",
            request.dateOfJoining(),
            new BigDecimal("750000"),
            EmploymentStatus.INACTIVE
        );
        when(repository.findByIdAndOwnerId(1L, null)).thenReturn(Optional.of(employee));
        when(repository.saveAndFlush(employee)).thenReturn(employee);
        var result = service.update(1, changes);
        assertThat(result.designation()).isEqualTo("Product Engineer");
        assertThat(result.department()).isEqualTo(Department.PRODUCT);
        assertThat(result.status()).isEqualTo(EmploymentStatus.INACTIVE);
        verify(repository).existsByEmailIgnoreCaseAndIdNotAndOwnerId(request.email(), 1L, null);
    }

    @Test
    void cannotChangeEmailToAnotherEmployeesEmail() {
        when(repository.findByIdAndOwnerId(1L, null)).thenReturn(Optional.of(employee));
        when(repository.existsByEmailIgnoreCaseAndIdNotAndOwnerId(request.email(), 1L, null)).thenReturn(true);
        assertThatThrownBy(() -> service.update(1, request)).isInstanceOf(DuplicateEmailException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void cannotUpdateMissingEmployee() {
        assertThatThrownBy(() -> service.update(99, request)).isInstanceOf(EmployeeNotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deletesExistingEmployee() {
        when(repository.findByIdAndOwnerId(1L, null)).thenReturn(Optional.of(employee));
        service.delete(1);
        verify(repository).delete(employee);
        verify(repository).flush();
    }

    @Test
    void cannotDeleteMissingEmployee() {
        assertThatThrownBy(() -> service.delete(99)).isInstanceOf(EmployeeNotFoundException.class);
        verify(repository, never()).delete(any(Employee.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesPaginationMetadataAndAddsStableSort() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(employee), org.springframework.data.domain.PageRequest.of(1, 5), 6)
        );
        var result = service.list("aditi", Department.ENGINEERING, EmploymentStatus.ACTIVE, 1, 5, "fullName", "desc");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(6);
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("fullName").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({
        "-1,10,fullName,asc",
        "0,0,fullName,asc",
        "0,101,fullName,asc",
        "2147483647,100,fullName,asc",
        "0,10,salary,asc",
        "0,10,fullName,sideways",
    })
    void rejectsInvalidPaginationAndSort(int page, int size, String sort, String direction) {
        assertThatThrownBy(() -> service.list(null, null, null, page, size, sort, direction)).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoInteractions(repository);
    }

    @Test
    void returnsDirectoryWideTotals() {
        when(repository.countByOwnerId(null)).thenReturn(3L);
        when(repository.countByStatusAndOwnerId(EmploymentStatus.ACTIVE, null)).thenReturn(2L);
        when(repository.countByStatusAndOwnerId(EmploymentStatus.INACTIVE, null)).thenReturn(1L);
        when(repository.countDepartments(null)).thenReturn(2L);
        assertThat(service.summary()).isEqualTo(new EmployeeSummary(3, 2, 1, 2));
    }
}
