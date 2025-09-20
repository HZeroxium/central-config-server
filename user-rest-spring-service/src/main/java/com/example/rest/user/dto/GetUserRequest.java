package com.example.rest.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for getting a user by ID.
 */
@Data
@Schema(name = "GetUserRequest", description = "Request payload to get a user by ID")
public class GetUserRequest {

    @Schema(description = "User ID", example = "user123", required = true)
    @NotBlank(message = "User ID is required")
    private String id;
}
