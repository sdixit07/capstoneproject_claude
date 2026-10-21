package org.ecom.productcatalog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.model.Category;
import org.ecom.productcatalog.repository.CategoryRepository;
import org.ecom.productcatalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductPaginationTest {

    private static final int N = 27;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    private Long firstCategoryId;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        List<Category> cats = new ArrayList<>();
        for (String n : List.of("A", "B", "C")) {
            Category c = new Category();
            c.setName(n);
            cats.add(categoryRepository.save(c));
        }
        firstCategoryId = cats.get(0).getId();
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            Product p = new Product();
            p.setName("P" + i);
            p.setDescription("d");
            p.setImageUrl("u");
            p.setPrice(1.0 + i);
            p.setCategory(cats.get(i % 3));
            products.add(p);
        }
        productRepository.saveAll(products);
    }

    private JsonNode body(String url) throws Exception {
        MvcResult r = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString());
    }

    @Test
    void defaultsApply() throws Exception {
        JsonNode j = body("/api/products");
        assertEquals(0, j.get("page").asInt());
        assertEquals(10, j.get("size").asInt());
        assertEquals(10, j.get("items").size());
        assertEquals(N, j.get("totalItems").asLong());
        assertEquals(3, j.get("totalPages").asInt());
    }

    @Test
    void firstPage() throws Exception {
        JsonNode j = body("/api/products?page=0&size=10");
        assertTrue(j.get("items").size() <= 10);
        assertTrue(j.get("totalItems").asLong() > 10);
        assertTrue(j.get("totalPages").asInt() > 1);
    }

    @Test
    void thirdPageIsPartial() throws Exception {
        JsonNode j = body("/api/products?page=2&size=10");
        assertEquals(2, j.get("page").asInt());
        assertEquals(N - 20, j.get("items").size());
    }

    @Test
    void negativePageIs400() throws Exception {
        mvc.perform(get("/api/products?page=-1&size=10")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidSizeIs400() throws Exception {
        mvc.perform(get("/api/products?page=0&size=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products?size=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products?size=101")).andExpect(status().isBadRequest());
    }

    @Test
    void sizeHundredIsOk() throws Exception {
        JsonNode j = body("/api/products?size=100");
        assertEquals(N, j.get("items").size());
    }

    @Test
    void nonNumericOrOutOfRangeIs400() throws Exception {
        mvc.perform(get("/api/products?page=abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products?size=abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products?page=99999999999")).andExpect(status().isBadRequest());
    }

    @Test
    void pastTheEndReturnsEmptyEnvelope() throws Exception {
        JsonNode j = body("/api/products?page=9&size=10");
        assertEquals(0, j.get("items").size());
        assertEquals(9, j.get("page").asInt());
        assertEquals(N, j.get("totalItems").asLong());
        assertEquals(3, j.get("totalPages").asInt());
    }

    @Test
    void offsetOverflowReturnsEmptyEnvelope() throws Exception {
        JsonNode j = body("/api/products?page=2147483647&size=10");
        assertEquals(0, j.get("items").size());
        assertEquals(2147483647, j.get("page").asInt());
        assertEquals(10, j.get("size").asInt());
        assertEquals(N, j.get("totalItems").asLong());
        assertEquals(3, j.get("totalPages").asInt());
    }

    @Test
    void justBelowGuardBoundaryIsOkAndEmpty() throws Exception {
        JsonNode j = body("/api/products?page=2147483647&size=1");
        assertEquals(0, j.get("items").size());
        assertEquals(N, j.get("totalItems").asLong());
    }

    @Test
    void emptyTable() throws Exception {
        productRepository.deleteAll();
        JsonNode j = body("/api/products");
        assertEquals(0, j.get("items").size());
        assertEquals(0, j.get("totalItems").asLong());
        assertEquals(0, j.get("totalPages").asInt());
    }

    @Test
    void walkingAllPagesYieldsAscendingGaplessIds() throws Exception {
        Set<Long> expected = new TreeSet<>();
        productRepository.findAll().forEach(p -> expected.add(p.getId()));
        for (int size : new int[]{10, 7}) {
            JsonNode first = body("/api/products?page=0&size=" + size);
            int totalPages = first.get("totalPages").asInt();
            long total = first.get("totalItems").asLong();
            long previous = Long.MIN_VALUE;
            Set<Long> seen = new HashSet<>();
            for (int p = 0; p < totalPages; p++) {
                JsonNode items = body("/api/products?page=" + p + "&size=" + size).get("items");
                for (JsonNode item : items) {
                    long id = item.get("id").asLong();
                    assertTrue(id > previous, "ids must be strictly ascending");
                    previous = id;
                    assertTrue(seen.add(id), "duplicate id " + id);
                }
            }
            assertEquals(total, seen.size());
            assertEquals(expected, new TreeSet<>(seen));
        }
    }

    @Test
    void responseShape() throws Exception {
        JsonNode j = body("/api/products");
        assertTrue(j.isObject());
        Set<String> keys = new TreeSet<>();
        Iterator<String> it = j.fieldNames();
        it.forEachRemaining(keys::add);
        assertEquals(new TreeSet<>(List.of("items", "page", "size", "totalItems", "totalPages")), keys);
    }

    @Test
    void itemsNeverExceedSize() throws Exception {
        assertEquals(5, body("/api/products?size=5").get("items").size());
    }

    @Test
    void categoryEndpointStillBareArray() throws Exception {
        JsonNode j = body("/api/products/category/" + firstCategoryId);
        assertTrue(j.isArray());
        assertEquals(N / 3, j.size());
    }

    @Test
    void categoriesEndpointUnchanged() throws Exception {
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
    }
}
