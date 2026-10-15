package com.kloudspot.assignment.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    @Profile("!cloud")
    SecurityFilterChain localSecurity(HttpSecurity http) throws Exception {
        // The local assessment profile stays open on loopback. Hosted deployments use cloud.
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }

    @Bean
    @Profile("!cloud")
    UserDetailsService localUsers() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @Profile("cloud")
    SecurityFilterChain cloudSecurity(HttpSecurity http) throws Exception {
        var loginEntryPoint = new LoginUrlAuthenticationEntryPoint("/login.html");
        return http
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers(
                        "/login.html",
                        "/register.html",
                        "/js/login.js",
                        "/js/register.js",
                        "/js/session.js",
                        "/css/styles.css",
                        "/api/csrf",
                        "/api/auth/register",
                        "/actuator/health",
                        "/error"
                    )
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/employees/**")
                    .hasAnyRole("ADMIN", "MEMBER", "VIEWER")
                    .requestMatchers("/api/employees/**")
                    .hasAnyRole("ADMIN", "MEMBER")
                    .anyRequest()
                    .authenticated()
            )
            .csrf(csrf ->
                csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new BrowserCsrfTokenRequestHandler())
            )
            .formLogin(login ->
                login
                    .loginPage("/login.html")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login.html?error")
                    .permitAll()
            )
            .logout(logout ->
                logout.logoutSuccessUrl("/login.html?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID")
            )
            .exceptionHandling(errors ->
                errors.authenticationEntryPoint((request, response, exception) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    } else {
                        loginEntryPoint.commence(request, response, exception);
                    }
                })
            )
            .build();
    }
}
