package com.supportdesk.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/** A stable JSON shape for one page of results (Spring's Page is not meant to be serialized as-is). */
public record PageResponse<T>(
        @Schema(description = "This page's rows.") List<T> items,
        @Schema(description = "This page, counting from 0.", example = "0") int page,
        @Schema(description = "Rows per page.", example = "8") int size,
        @Schema(description = "Rows on every page together.", example = "9") long totalItems,
        @Schema(description = "Number of pages.", example = "2") int totalPages)
{
    public static <T> PageResponse<T> of(Page<T> page)
    {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
