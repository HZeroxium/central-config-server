package com.example.user.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain aggregate representing a user in the system.
 * This model is independent of transport (REST/Thrift) and persistence (JPA/Mongo) concerns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "User", description = "Domain user entity for RPC service")
public class User {
  @Schema(description = "Unique identifier", example = "u-1")
  private String id;

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
