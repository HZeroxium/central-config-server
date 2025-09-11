package com.example.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/** Response view returned by the API. */
@Value
@Builder
@Schema(name = "UserResponse", description = "User view returned by the API")
public class UserResponse {
  @Schema(description = "Unique identifier", example = "u-1")
  String id;

  @Schema(description = "Full name", example = "Alice Nguyen")
  String name;

  @Schema(description = "Phone number", example = "+84-912-345-678")
  String phone;

  @Schema(description = "Address", example = "123 Nguyen Trai, Ha Noi")
  String address;
}


