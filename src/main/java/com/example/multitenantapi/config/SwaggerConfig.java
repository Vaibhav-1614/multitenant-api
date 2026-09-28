package com.example.multitenantapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Multitenant API")
                        .version("v1")
                        .description("Tenant-isolated project management API. "
                                + "1) POST /api/auth/register to create a tenant and its first ADMIN, "
                                + "2) click Authorize and paste the accessToken, "
                                + "3) every project call is automatically scoped to your tenant."))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .schemaRequirement(
                        schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                );
    }
}
