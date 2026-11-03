package org.ecom.productcatalog.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <T> PagedResponse<T> from(Page<T> source) {
        return new PagedResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages());
    }
}
