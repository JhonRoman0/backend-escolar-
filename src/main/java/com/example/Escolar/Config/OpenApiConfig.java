package com.example.Escolar.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Value("${app.swagger.authorize:true}")
    private boolean swaggerAuthorize;

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI api = new OpenAPI()
                .info(new Info()
                        .title("Escolar API")
                        .version("1.0")
                        .description("API de gestion escolar"));
        if (swaggerAuthorize) {
            api.components(new Components()
                    .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")))
                    .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
        }
        return api;
    }
}