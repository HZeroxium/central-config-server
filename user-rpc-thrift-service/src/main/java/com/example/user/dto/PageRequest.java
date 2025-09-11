package com.example.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Value;

/** Pagination request parameters. */
@Value
@Schema(name = "PageRequest", description = "Pagination request parameters")
public class PageRequest {
  @Schema(description = "Zero-based page index", example = "0")
  @Min(0)
  int page;

  @Schema(description = "Page size (1..200)", example = "20")
  @Min(1)
  @Max(200)
  int size;
}


