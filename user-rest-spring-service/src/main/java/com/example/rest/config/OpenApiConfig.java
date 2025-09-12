package com.example.rest.config;

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
            title = "User REST API",
            version = "1.0.0",
            description = "REST facade for User RPC (Thrift) service",
            contact = @Contact(name = "Thrift Demo", email = "dev@example.com"),
            license =
                @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")),
    // Use relative server to match whatever host/port the app is served from (avoids CORS/mismatch)
    servers = { @Server(url = "/", description = "Current host") })
public class OpenApiConfig { }
