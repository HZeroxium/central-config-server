package com.example.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "UserRequest", description = "Payload to create/update a user")
public class UserRequest {
  @Schema(description = "Full name", example = "Alice Nguyen")
  @NotBlank
  @Size(max = 100)
  private String name;

  @Schema(description = "Phone number", example = "+84-912-345-678")
  @NotBlank
  @Size(max = 32)
  private String phone;

  @Schema(description = "Address", example = "123 Nguyen Trai, Ha Noi")
  @Size(max = 255)
  private String address;
}


