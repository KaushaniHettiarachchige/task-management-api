package com.example.taskmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI taskManagementOpenAPI() {
        String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Task Management REST API")
                        .version("1.0.0.")
                        .description(
                            "Secure Task Management API built with "
                            + "Java 25, Spring Boot 4, "
                            + "Spring Security, JWT and MySQL. "
                            + "Users can register, log in and "
                            + "manage their own tasks."
                        )
                )
                .components(new Components()
                        .addSecuritySchemes(
                                securitySchemeName, 
                                new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                        )
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(securitySchemeName)
                );
    }
}
