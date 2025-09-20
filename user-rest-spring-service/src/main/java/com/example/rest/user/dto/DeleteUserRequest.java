package com.example.rest.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for deleting a user.
 */
@Data
@Schema(name = "DeleteUserRequest", description = "Request payload to delete a user")
public class DeleteUserRequest {

    @Schema(description = "User ID", example = "user123", required = true)
    @NotBlank(message = "User ID is required")
    private String id;
}
