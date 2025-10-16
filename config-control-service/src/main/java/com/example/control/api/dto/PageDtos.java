package com.example.control.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic page response DTO to wrap paginated results.
 */
public final class PageDtos {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PageResponse<T> {
    private List<T> items;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private boolean hasNext;
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


