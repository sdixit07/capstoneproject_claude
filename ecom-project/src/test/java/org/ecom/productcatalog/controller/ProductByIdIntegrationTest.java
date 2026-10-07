package org.ecom.productcatalog.controller;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.model.Category;
import org.ecom.productcatalog.repository.CategoryRepository;
import org.ecom.productcatalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductByIdIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;

    Long catId;
    Long productId;

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category cat = new Category();
        cat.setName("Electronics");
        cat = categoryRepository.save(cat);
        catId = cat.getId();

        Product p = new Product();
        p.setName("Phone");
        p.setDescription("A phone");
        p.setImageUrl("http://img/phone.png");
        p.setPrice(199.5);
        p.setCategory(cat);
        productId = productRepository.save(p).getId();

        Product q = new Product();
        q.setName("Tablet");
        q.setDescription("A tablet");
        q.setImageUrl("http://img/tablet.png");
        q.setPrice(299.0);
        q.setCategory(cat);
        productRepository.save(q);
    }

    @Test
    void getById_existing_returns200WithDetails() throws Exception {
        mockMvc.perform(get("/api/products/{id}", productId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Phone"))
                .andExpect(jsonPath("$.description").value("A phone"))
                .andExpect(jsonPath("$.imageUrl").value("http://img/phone.png"))
                .andExpect(jsonPath("$.price").value(199.5))
                .andExpect(jsonPath("$.category.id").value(catId))
                .andExpect(jsonPath("$.category.name").value("Electronics"));
    }

    @Test
    void getById_unknown_returns404() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(result -> {
                    ResponseStatusException ex = assertInstanceOf(ResponseStatusException.class, result.getResolvedException());
                    assertEquals("Product not found with id 999999", ex.getReason());
                });
    }

    @Test
    void getById_zeroAndNegative_return404() throws Exception {
        mockMvc.perform(get("/api/products/0")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/products/-1")).andExpect(status().isNotFound());
    }

    @Test
    void getById_nonNumeric_returns400() throws Exception {
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class, result.getResolvedException()));
    }

    @Test
    void getById_outOfRange_returns400() throws Exception {
        mockMvc.perform(get("/api/products/99999999999999999999"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class, result.getResolvedException()));
    }

    @Test
    void categoryWithoutId_returns400_documentedBehaviourChange() throws Exception {
        mockMvc.perform(get("/api/products/category")).andExpect(status().isBadRequest());
    }

    @Test
    void categoryPath_stillReturnsList() throws Exception {
        mockMvc.perform(get("/api/products/category/{id}", catId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void listAll_unchanged() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getById_allowsConfiguredCorsOrigin() throws Exception {
        mockMvc.perform(get("/api/products/{id}", productId).header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
