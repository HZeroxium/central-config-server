package com.example.rest.user.dto;

import com.example.common.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for updating a user.
 */
@Data
@Schema(name = "UpdateUserRequest", description = "Request payload to update a user")
public class UpdateUserRequest {
    
    @Schema(description = "Full name", example = "Alice Nguyen Updated", required = true)
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Schema(description = "Phone number", example = "+84-912-345-678", required = true)
    @NotBlank(message = "Phone is required")
    @Size(max = 32, message = "Phone must not exceed 32 characters")
    private String phone;

    @Schema(description = "Address", example = "123 Nguyen Trai, Ha Noi")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Schema(description = "User status", example = "ACTIVE")
    private User.UserStatus status;

    @Schema(description = "User role", example = "USER")
    private User.UserRole role;

    @Schema(description = "Version for optimistic locking", example = "1")
    private Integer version;
}
