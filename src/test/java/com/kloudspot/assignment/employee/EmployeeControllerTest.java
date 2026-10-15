package com.kloudspot.assignment.employee;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kloudspot.assignment.employee.DuplicateEmailException;
import com.kloudspot.assignment.employee.EmployeeNotFoundException;
import com.kloudspot.assignment.employee.dto.EmployeeResponse;
import com.kloudspot.assignment.security.SecurityConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
@Import(SecurityConfig.class)
class EmployeeControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockitoBean
    EmployeeService service;

    static final String VALID_JSON = """
    {"fullName":"Aditi Sharma","email":"aditi@example.com","phoneNumber":"+91 98765 43210",
     "department":"ENGINEERING","designation":"Backend Engineer","dateOfJoining":"2024-06-10",
     "salary":650000.50,"status":"ACTIVE"}
    """;

    @Test
    void creationReturns201AndLocation() throws Exception {
        when(service.create(any())).thenReturn(
            new EmployeeResponse(
                1L,
                "Aditi Sharma",
                "aditi@example.com",
                "+91 98765 43210",
                Department.ENGINEERING,
                "Backend Engineer",
                LocalDate.of(2024, 6, 10),
                new BigDecimal("650000.50"),
                EmploymentStatus.ACTIVE
            )
        );
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/employees/1"))
            .andExpect(jsonPath("$.id").value(1));
    }

    @ParameterizedTest
    @CsvSource({ "fullName,   ", "email,not-an-email", "phoneNumber,abc", "designation,   " })
    void invalidTextProducesFieldErrors(String field, String invalid) throws Exception {
        String json = VALID_JSON.replaceAll(
            "\"" + field + "\":\"[^\"]*\"",
            "\"" + field + "\":\"" + (invalid == null ? " " : invalid) + "\""
        );
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors." + field).exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsNegativeSalary() throws Exception {
        mvc.perform(
            post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_JSON.replace("650000.50", "-1"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.salary").exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() throws Exception {
        mvc.perform(
            post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_JSON.replace("650000.50", "12.345"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.salary").exists());
    }

    @ParameterizedTest
    @CsvSource({ "fullName,121", "email,255", "phoneNumber,33", "designation,121" })
    void rejectsOversizedText(String field, int length) throws Exception {
        var body = mapper.readTree(VALID_JSON);
        ((com.fasterxml.jackson.databind.node.ObjectNode) body).put(field, "a".repeat(length));
        mvc.perform(
            post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors." + field).exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsSalaryBeyondDatabasePrecision() throws Exception {
        mvc.perform(
            post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_JSON.replace("650000.50", "10000000000"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.salary").exists());
    }

    @Test
    void rejectsUnknownPropertiesInsteadOfSilentlyIgnoringThem() throws Exception {
        mvc.perform(
            post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_JSON.replace("{", "{\"salery\":5,"))
        ).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsFutureJoiningDate() throws Exception {
        mvc.perform(
            post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_JSON.replace("2024-06-10", LocalDate.now().plusDays(1).toString()))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.dateOfJoining").exists());
    }

    @Test
    void rejectsInvalidEnumAndMalformedJson() throws Exception {
        for (String body : new String[] {
            VALID_JSON.replace("ACTIVE", "INVALID"),
            "{broken",
            VALID_JSON.replace("2024-06-10", "2024-02-30"),
        }) {
            mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(
                status().isBadRequest()
            );
        }
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingFields() throws Exception {
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.fullName").exists())
            .andExpect(jsonPath("$.errors.salary").exists())
            .andExpect(jsonPath("$.errors.status").exists());
    }

    @Test
    void duplicateEmailProduces409AndFieldError() throws Exception {
        when(service.create(any())).thenThrow(new DuplicateEmailException());
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void missingEmployeeProduces404() throws Exception {
        when(service.get(99L)).thenThrow(new EmployeeNotFoundException(99L));
        mvc.perform(get("/api/employees/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteReturns204WithNoBody() throws Exception {
        mvc.perform(delete("/api/employees/1")).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).delete(1L);
    }

    @Test
    void rejectsInvalidQueryValues() throws Exception {
        mvc.perform(get("/api/employees").param("status", "INVALID")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/employees").param("page", "abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/employees").param("search", "a".repeat(121))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
