package com.kloudspot.assignment.employee;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kloudspot.assignment.employee.dto.EmployeeRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
class EmployeeApiIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    EmployeeRepository repository;

    private Employee seed(String name, String email, Department department, EmploymentStatus status) {
        return repository.saveAndFlush(
            new Employee(
                new EmployeeRequest(
                    name,
                    email,
                    "+91 98765 43210",
                    department,
                    "Engineer",
                    LocalDate.of(2024, 1, 1),
                    new BigDecimal("600000.50"),
                    status
                )
            )
        );
    }

    @Test
    void fullCrudLifecycleUsesPostgresql() throws Exception {
        var created = mvc
            .perform(
                post("/api/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(EmployeeControllerTest.VALID_JSON)
            )
            .andExpect(status().isCreated())
            .andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/employees/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("aditi@example.com"));
        mvc.perform(
            put("/api/employees/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    EmployeeControllerTest.VALID_JSON.replace("Backend Engineer", "Senior Engineer").replace(
                        "ACTIVE",
                        "INACTIVE"
                    )
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.designation").value("Senior Engineer"));
        mvc.perform(delete("/api/employees/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/employees/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void combinesCaseInsensitiveSearchDepartmentStatusAndPagination() throws Exception {
        seed("Aditi One", "one@example.com", Department.ENGINEERING, EmploymentStatus.ACTIVE);
        seed("Aditi Two", "two@example.com", Department.ENGINEERING, EmploymentStatus.ACTIVE);
        seed("Aditi Inactive", "inactive@example.com", Department.ENGINEERING, EmploymentStatus.INACTIVE);
        seed("Aditi Designer", "designer@example.com", Department.DESIGN, EmploymentStatus.ACTIVE);
        mvc.perform(
            get("/api/employees")
                .param("search", "ADITI")
                .param("department", "ENGINEERING")
                .param("status", "ACTIVE")
                .param("size", "1")
                .param("page", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content[0].fullName").value("Aditi Two"));
        mvc.perform(get("/api/employees").param("search", "ONE@EXAMPLE.COM")).andExpect(
            jsonPath("$.totalElements").value(1)
        );
    }

    @Test
    void escapesSqlWildcardsInSearch() throws Exception {
        seed("Aditi One", "one@example.com", Department.ENGINEERING, EmploymentStatus.ACTIVE);
        seed("Team_100%", "percent@example.com", Department.DESIGN, EmploymentStatus.ACTIVE);
        for (String search : new String[] { "%", "_" }) {
            mvc.perform(get("/api/employees").param("search", search))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Team_100%"));
        }
    }

    @Test
    void duplicateEmailsAreCaseInsensitive() throws Exception {
        seed("Aditi", "aditi@example.com", Department.ENGINEERING, EmploymentStatus.ACTIVE);
        mvc.perform(
            post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(EmployeeControllerTest.VALID_JSON.replace("aditi@example.com", "ADITI@EXAMPLE.COM"))
        ).andExpect(status().isConflict());
    }

    @Test
    void databaseUniquenessAlsoProtectsWritesOutsideTheService() {
        seed("Aditi", "aditi@example.com", Department.ENGINEERING, EmploymentStatus.ACTIVE);
        assertThatThrownBy(() ->
            seed("Other", "aditi@example.com", Department.FINANCE, EmploymentStatus.ACTIVE)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emptyAndOutOfRangePagesHaveStableShape() throws Exception {
        mvc.perform(get("/api/employees").param("page", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.page").value(50))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void openApiAndFrontendAreServedByTheApplication() throws Exception {
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/employees']").exists());
        mvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
