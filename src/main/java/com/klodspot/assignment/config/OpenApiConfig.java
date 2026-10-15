package com.klodspot.assignment.config;

import com.klodspot.assignment.employee.Employee;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI employeeApi() {
        return new OpenAPI().info(
            new Info()
                .title("Klodspot Assignment API")
                .version("1.0.0")
                .description("Employee directory. Salary is annual gross pay in INR. IDs are assigned by the database.")
        );
    }
}
