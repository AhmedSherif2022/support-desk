package com.supportdesk.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** A stable JSON shape for one page of results (Spring's Page is not meant to be serialized as-is). */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages)
{
    public static <T> PageResponse<T> of(Page<T> page)
    {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
