package com.example.control.api.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic page response DTO to wrap paginated results.
 * <p>
 * Provides consistent pagination metadata for all paginated endpoints,
 * following Spring Data Page conventions.
 * </p>
 */
public final class PageDtos {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Paginated response with Spring Data Page metadata")
  public static class PageResponse<T> {
    @Schema(description = "List of items in current page")
    private List<T> items;
    
    @Schema(description = "Total number of elements across all pages", example = "150")
    private long totalElements;
    
    @Schema(description = "Total number of pages", example = "8")
    private int totalPages;
    
    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;
    
    @Schema(description = "Number of items per page", example = "20")
    private int size;
    
    @Schema(description = "Whether there are more pages available", example = "true")
    private boolean hasNext;
    
    @Schema(description = "Whether there are previous pages available", example = "false")
    private boolean hasPrevious;

    public static <T> PageResponse<T> from(Page<T> page) {
      return PageResponse.<T>builder()
          .items(page.getContent())
          .totalElements(page.getTotalElements())
          .totalPages(page.getTotalPages())
          .page(page.getNumber())
          .size(page.getSize())
          .hasNext(page.hasNext())
          .hasPrevious(page.hasPrevious())
          .build();
    }
  }
}


