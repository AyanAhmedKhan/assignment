package com.kloudspot.assignment.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = { "app.admin.username=reviewer", "app.admin.password=Test-only-password-38!" })
@AutoConfigureMockMvc
@ActiveProfiles({ "cloud", "integration" })
@Transactional
class CloudSecurityIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Test
    void anonymousUsersCannotReadEmployeeDataOrApiDocumentation() throws Exception {
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/employees/summary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login.html"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().is3xxRedirection());
    }

    @Test
    void healthIsPublicWithoutDatabaseDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/login.html"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void loginRejectsIncorrectCredentials() throws Exception {
        var initial = mvc.perform(get("/api/csrf")).andReturn();
        var cookie = initial.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        mvc.perform(
            post("/login")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .param("username", "reviewer")
                .param("password", "wrong-password")
        ).andExpect(redirectedUrl("/login.html?error"));
    }

    @Test
    void writesRequireCsrfAndAnAdministrator() throws Exception {
        mvc.perform(delete("/api/employees/1").with(user("reviewer").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/employees").with(user("reader").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(
            delete("/api/employees/1").with(user("reviewer").roles("ADMIN")).header("X-XSRF-TOKEN", "invalid-token")
        ).andExpect(status().isForbidden());
    }

    @Test
    void browserCanLoginWithMaskedTokenAndWriteUsingCookieHeader() throws Exception {
        var initial = mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        var token = mapper.readTree(initial.getResponse().getContentAsString());
        var cookie = initial.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        var login = mvc
            .perform(
                post("/login")
                    .cookie(cookie)
                    .param(token.get("parameterName").asText(), token.get("token").asText())
                    .param("username", "reviewer")
                    .param("password", "Test-only-password-38!")
            )
            .andExpect(redirectedUrl("/"))
            .andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/api/employees").session(session)).andExpect(status().isOk());

        var refreshed = mvc.perform(get("/api/csrf").session(session)).andReturn();
        var refreshedCookie = refreshed.getResponse().getCookie("XSRF-TOKEN");
        assertThat(refreshedCookie).isNotNull();
        var created = mvc
            .perform(
                post("/api/employees")
                    .session(session)
                    .cookie(refreshedCookie)
                    .header("X-XSRF-TOKEN", refreshedCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"fullName":"Security Test", "email":"security@example.com",
                         "phoneNumber":"+91 98765 43210", "department":"ENGINEERING",
                         "designation":"Engineer", "dateOfJoining":"2024-01-01",
                         "salary":500000, "status":"ACTIVE"}
                        """
                    )
            )
            .andExpect(status().isCreated())
            .andReturn();
        var id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(
            delete("/api/employees/" + id)
                .session(session)
                .cookie(refreshedCookie)
                .header("X-XSRF-TOKEN", refreshedCookie.getValue())
        ).andExpect(status().isNoContent());
        mvc.perform(
            post("/logout").session(session).cookie(refreshedCookie).header("X-XSRF-TOKEN", refreshedCookie.getValue())
        ).andExpect(redirectedUrl("/login.html?logout"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
    }
}
