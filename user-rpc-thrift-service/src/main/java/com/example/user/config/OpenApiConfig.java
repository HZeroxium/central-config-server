package com.example.user.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "User RPC - Internal REST",
            version = "1.0.0",
            description = "Internal REST endpoints for CRUD to validate logic",
            contact = @Contact(name = "Thrift Demo", email = "dev@example.com"),
            license =
                @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")),
    servers = { @Server(url = "http://localhost:8080", description = "Local") })
public class OpenApiConfig { }


