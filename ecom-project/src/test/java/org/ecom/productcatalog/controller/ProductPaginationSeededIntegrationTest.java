package org.ecom.productcatalog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the pagination ACs against the real DataSeeder dataset (no test-owned data). */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductPaginationSeededIntegrationTest {

    @Autowired
    private MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode body(String url) throws Exception {
        return mapper.readTree(mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    void seededDefaultsAndFirstPage() throws Exception {
        JsonNode j = body("/api/products");
        assertEquals(0, j.get("page").asInt());
        assertEquals(10, j.get("size").asInt());
        assertEquals(10, j.get("items").size());
        assertTrue(j.get("totalItems").asLong() >= 25);
        assertTrue(j.get("totalPages").asInt() >= 3);
    }

    @Test
    void seededThirdPageNonEmpty() throws Exception {
        JsonNode j = body("/api/products?page=2&size=10");
        assertEquals(2, j.get("page").asInt());
        assertTrue(j.get("items").size() >= 1 && j.get("items").size() <= 10);
    }

    @Test
    void seededIdsAscendingAcrossPagesWithoutDuplicates() throws Exception {
        long prev = Long.MIN_VALUE;
        Set<Long> seen = new HashSet<>();
        int pages = body("/api/products").get("totalPages").asInt();
        for (int p = 0; p < pages; p++) {
            for (JsonNode item : body("/api/products?page=" + p + "&size=10").get("items")) {
                long id = item.get("id").asLong();
                assertTrue(id > prev);
                prev = id;
                assertTrue(seen.add(id));
            }
        }
        assertEquals(body("/api/products").get("totalItems").asLong(), seen.size());
    }

    @Test
    void seededBoundaryAndInvalidInputs() throws Exception {
        assertEquals(body("/api/products").get("totalItems").asLong(), body("/api/products?size=100").get("items").size());
        mvc.perform(get("/api/products?size=101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products?page=-1&size=10")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products?page=0&size=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products?page=abc")).andExpect(status().isBadRequest());
        JsonNode past = body("/api/products?page=50&size=10");
        assertEquals(0, past.get("items").size());
        assertTrue(past.get("totalItems").asLong() >= 25);
    }
}
