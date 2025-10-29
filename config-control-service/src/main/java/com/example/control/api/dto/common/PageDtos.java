package com.example.control.api.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * Generic page response DTO to wrap paginated results.
 * <p>
 * Provides consistent pagination metadata for all paginated endpoints,
 * following Spring Data Page conventions.
 * </p>
 */
public final class PageDtos {

    private PageDtos() {
        throw new UnsupportedOperationException("Utility class");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Pagination metadata from Spring Data Page")
    public static class PageMetadata {
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

        @Schema(description = "Whether this is the first page", example = "true")
        private boolean isFirst;

        @Schema(description = "Whether this is the last page", example = "false")
        private boolean isLast;

        @Schema(description = "Number of elements in current page", example = "20")
        private int numberOfElements;

        @Schema(description = "Whether the page is empty", example = "false")
        private boolean empty;

        public static PageMetadata from(Page<?> page) {
            return PageMetadata.builder()
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .page(page.getNumber())
                    .size(page.getSize())
                    .hasNext(page.hasNext())
                    .hasPrevious(page.hasPrevious())
                    .isFirst(page.isFirst())
                    .isLast(page.isLast())
                    .numberOfElements(page.getNumberOfElements())
                    .empty(page.isEmpty())
                    .build();
        }
    }
}
