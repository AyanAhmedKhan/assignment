package com.kloudspot.assignment.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kloudspot.assignment.employee.Department;
import com.kloudspot.assignment.employee.Employee;
import com.kloudspot.assignment.employee.EmployeeRepository;
import com.kloudspot.assignment.employee.EmploymentStatus;
import com.kloudspot.assignment.employee.dto.EmployeeRequest;
import com.kloudspot.assignment.security.AccountPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = { "app.admin.username=reviewer", "app.admin.password=Test-only-password-38!" })
@AutoConfigureMockMvc
@ActiveProfiles({ "cloud", "integration" })
@Transactional
class AccountSecurityIT {

    private static final String PASSWORD = "A good test password 42!";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    AccountRepository accounts;

    @Autowired
    EmployeeRepository employees;

    @Autowired
    PasswordEncoder encoder;

    private String registration(String email) throws Exception {
        return mapper.writeValueAsString(new RegistrationRequest(" New Member ", email, PASSWORD, PASSWORD));
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request) throws Exception {
        var result = mvc.perform(get("/api/csrf")).andReturn();
        var cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return request.cookie(cookie).header("X-XSRF-TOKEN", cookie.getValue());
    }

    private Account account(String email, AccountRole role) {
        return accounts.saveAndFlush(new Account(email, email, "Test Member", encoder.encode(PASSWORD), role, false));
    }

    private EmployeeRequest employeeRequest(String email) {
        return new EmployeeRequest(
            "Workspace Employee",
            email,
            "+91 98765 43210",
            Department.ENGINEERING,
            "Engineer",
            LocalDate.of(2024, 1, 1),
            new BigDecimal("650000.50"),
            EmploymentStatus.ACTIVE
        );
    }

    @Test
    void demoLoginIsDisabledWhenTheDemoProfileIsOff() throws Exception {
        accounts.saveAndFlush(
            new Account(
                "disabled-demo",
                "disabled-demo@example.com",
                "Demo",
                encoder.encode(PASSWORD),
                AccountRole.MEMBER,
                true
            )
        );
        mvc.perform(withToken(post("/login")).param("username", "disabled-demo").param("password", PASSWORD)).andExpect(
            redirectedUrl("/login.html?error")
        );
    }

    @Test
    void registrationStoresAHashAndEmailLoginOpensAnEmptyDirectory() throws Exception {
        mvc.perform(
            withToken(post("/api/auth/register"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(registration(" NEW@example.com "))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.password").doesNotExist());
        var account = accounts.findByUsernameIgnoreCase("new@example.com").orElseThrow();
        assertThat(account.getDisplayName()).isEqualTo("New Member");
        assertThat(account.getRole()).isEqualTo(AccountRole.MEMBER);
        assertThat(account.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, account.getPasswordHash())).isTrue();
        assertThat(account.isDemo()).isFalse();

        var login = mvc
            .perform(withToken(post("/login")).param("username", "NEW@EXAMPLE.COM").param("password", PASSWORD))
            .andExpect(redirectedUrl("/"))
            .andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/employees").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/session").session(session))
            .andExpect(jsonPath("$.displayName").value("New Member"))
            .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @ParameterizedTest
    @CsvSource({
        "fullName,   ",
        "fullName,''",
        "email,not-an-email",
        "password,short",
        "confirmPassword,different-password",
    })
    void invalidRegistrationShowsFieldErrors(String field, String value) throws Exception {
        var body = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(
            registration("invalid@example.com")
        );
        body.put(field, value == null ? " " : value);
        mvc.perform(
            withToken(post("/api/auth/register")).contentType(MediaType.APPLICATION_JSON).content(body.toString())
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors." + field).exists());
        assertThat(accounts.existsByEmailIgnoreCase("invalid@example.com")).isFalse();
    }

    @Test
    void rejectsPasswordsBeyondBcryptByteLimit() throws Exception {
        String password = "界".repeat(30);
        String body = mapper.writeValueAsString(
            new RegistrationRequest("Member", "bytes@example.com", password, password)
        );
        mvc.perform(withToken(post("/api/auth/register")).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void registrationRequiresCsrfAndRejectsDuplicateEmailsAndRoleInjection() throws Exception {
        mvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration("same@example.com"))
        ).andExpect(status().isForbidden());
        account("same@example.com", AccountRole.MEMBER);
        mvc.perform(
            withToken(post("/api/auth/register"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(registration("SAME@EXAMPLE.COM"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists());
        var body = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(
            registration("admin-attempt@example.com")
        );
        body.put("role", "ADMIN");
        mvc.perform(
            withToken(post("/api/auth/register")).contentType(MediaType.APPLICATION_JSON).content(body.toString())
        ).andExpect(status().isBadRequest());
    }

    @Test
    void allEmployeeReadsAndWritesStayInsideTheSignedInAccountsDirectory() throws Exception {
        var owner = new AccountPrincipal(account("owner@example.com", AccountRole.MEMBER));
        var other = new AccountPrincipal(account("other@example.com", AccountRole.MEMBER));
        String body = mapper.writeValueAsString(employeeRequest("shared-employee@example.com"));
        var created = mvc
            .perform(
                withToken(post("/api/employees"))
                    .with(user(owner))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ownerId").doesNotExist())
            .andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/employees").with(user(other)).param("search", "Workspace")).andExpect(
            jsonPath("$.totalElements").value(0)
        );
        mvc.perform(get("/api/employees/summary").with(user(other)))
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.departments").value(0));
        mvc.perform(get("/api/employees/" + id).with(user(other))).andExpect(status().isNotFound());
        mvc.perform(
            withToken(put("/api/employees/" + id))
                .with(user(other))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isNotFound());
        mvc.perform(withToken(delete("/api/employees/" + id)).with(user(other))).andExpect(status().isNotFound());
        // Two private directories can contain the same employee email.
        mvc.perform(
            withToken(post("/api/employees")).with(user(other)).contentType(MediaType.APPLICATION_JSON).content(body)
        ).andExpect(status().isCreated());
        mvc.perform(get("/api/employees").with(user(owner))).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/employees/summary").with(user(other))).andExpect(jsonPath("$.total").value(1));
        var admin = new AccountPrincipal(accounts.findByUsernameIgnoreCase("reviewer").orElseThrow());
        mvc.perform(get("/api/employees/" + id).with(user(admin))).andExpect(status().isNotFound());
        var injected = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(body);
        injected.put("ownerId", owner.getAccountId());
        mvc.perform(
            withToken(post("/api/employees"))
                .with(user(other))
                .contentType(MediaType.APPLICATION_JSON)
                .content(injected.toString())
        ).andExpect(status().isBadRequest());
    }

    @Test
    void viewerCanReadItsDirectoryButCannotWriteEvenWithAValidCsrfToken() throws Exception {
        var viewer = new AccountPrincipal(account("read-only@example.com", AccountRole.VIEWER));
        var employee = employees.saveAndFlush(
            new Employee(employeeRequest("viewer-employee@example.com"), viewer.getAccountId())
        );
        mvc.perform(get("/api/employees/" + employee.getId()).with(user(viewer))).andExpect(status().isOk());
        String body = mapper.writeValueAsString(employeeRequest("new@example.com"));
        mvc.perform(
            withToken(post("/api/employees")).with(user(viewer)).contentType(MediaType.APPLICATION_JSON).content(body)
        ).andExpect(status().isForbidden());
        mvc.perform(
            withToken(put("/api/employees/" + employee.getId()))
                .with(user(viewer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isForbidden());
        mvc.perform(withToken(delete("/api/employees/" + employee.getId())).with(user(viewer))).andExpect(
            status().isForbidden()
        );
    }
}
