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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductByIdErrorBodyIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        Category c = new Category();
        c.setName("Books");
        c = categoryRepository.save(c);
        Product p = new Product();
        p.setName("Novel");
        p.setDescription("A novel");
        p.setPrice(9.99);
        p.setCategory(c);
        productRepository.save(p);
    }

    private JsonNode assertErrorBody(String path, HttpStatus expected, String error) throws Exception {
        ResponseEntity<String> resp = rest.getForEntity(path, String.class);
        assertEquals(expected, resp.getStatusCode());
        JsonNode body = mapper.readTree(resp.getBody());
        for (String key : new String[]{"timestamp", "status", "error", "message", "path"}) {
            assertTrue(body.has(key), "missing key " + key);
        }
        assertEquals(expected.value(), body.get("status").asInt());
        assertEquals(error, body.get("error").asText());
        assertFalse(body.get("message").asText().isEmpty());
        assertEquals(path, body.get("path").asText());
        return body;
    }

    @Test
    void notFound_hasStandardErrorBodyWithMessage() throws Exception {
        JsonNode body = assertErrorBody("/api/products/999999", HttpStatus.NOT_FOUND, "Not Found");
        assertTrue(body.get("message").asText().contains("Product not found with id 999999"));
    }

    @Test
    void badRequest_hasStandardErrorBodyWithMessage() throws Exception {
        assertErrorBody("/api/products/abc", HttpStatus.BAD_REQUEST, "Bad Request");
    }
}
