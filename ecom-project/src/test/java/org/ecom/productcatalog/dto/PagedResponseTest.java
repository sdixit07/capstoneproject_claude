package org.ecom.productcatalog.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagedResponseTest {

    @Test
    void fromMapsAllFields() {
        PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(2, 2), 7);
        PagedResponse<String> r = PagedResponse.from(page);
        assertEquals(List.of("a", "b"), r.items());
        assertEquals(2, r.page());
        assertEquals(2, r.size());
        assertEquals(7, r.totalItems());
        assertEquals(4, r.totalPages());
    }

    @Test
    void fromEmptyPage() {
        PagedResponse<String> r = PagedResponse.from(new PageImpl<>(List.<String>of(), PageRequest.of(0, 10), 0));
        assertTrue(r.items().isEmpty());
        assertEquals(0, r.page());
        assertEquals(10, r.size());
        assertEquals(0, r.totalItems());
        assertEquals(0, r.totalPages());
    }
}
