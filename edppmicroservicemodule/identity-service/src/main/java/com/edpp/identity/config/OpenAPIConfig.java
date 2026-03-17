package com.edpp.identity.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Identity Service API")
                        .description("""
                                Customer Identity Management Service (CIF)
                                
                                Features:
                                * Customer onboarding with CIF number generation
                                * BVN (Bank Verification Number) validation
                                * NIN (National Identification Number) validation
                                * KYC verification
                                * Multitenancy support
                                * Fraud detection
                                * Audit logging
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Payment Platform Team")
                                .email("support@paymentplatform.com")
                                .url("https://paymentplatform.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://paymentplatform.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort + "/api")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://dev.paymentplatform.com/api")
                                .description("Development Server"),
                        new Server()
                                .url("https://staging.paymentplatform.com/api")
                                .description("Staging Server")
                ))
                .tags(List.of(
                        new Tag().name("Customer Management").description("Customer onboarding and management"),
                        new Tag().name("KYC Management").description("KYC verification endpoints"),
                        new Tag().name("Identity Verification").description("BVN and NIN verification"),
                        new Tag().name("Admin Operations").description("Administrative operations")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .name("Bearer Authentication")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Please enter JWT token with 'Bearer ' prefix"))
                        .addSecuritySchemes("Tenant-ID",
                                new SecurityScheme()
                                        .name("X-Tenant-ID")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Tenant identifier header")));
    }
}