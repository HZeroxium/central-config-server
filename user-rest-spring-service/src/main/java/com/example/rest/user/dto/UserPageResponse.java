package com.example.rest.user.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/** Paged response of users for REST facade. */
@Value
@Builder
@Schema(name = "UserPageResponse", description = "Paged users response")
public class UserPageResponse {
  @ArraySchema(schema = @Schema(implementation = UserResponse.class))
  List<UserResponse> items;

  @Schema(description = "Zero-based page index", example = "0")
  int page;

  @Schema(description = "Page size", example = "20")
  int size;

  @Schema(description = "Total number of items", example = "123")
  long total;

  @Schema(description = "Total number of pages", example = "7")
  int totalPages;
}


