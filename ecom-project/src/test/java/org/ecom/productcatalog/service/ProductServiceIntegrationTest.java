package org.ecom.productcatalog.service;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Exercises ProductService.getProductById against the real DataSeeder H2 data (AC-1, AC-2). */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Test
    void ac1_returnsSeededProductWhenFound() {
        List<Product> all = productService.getAllProducts();
        Product expected = all.get(0);
        Product actual = productService.getProductById(expected.getId());
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
    }

    @Test
    void ac2_throwsWithExactMessageWhenNotFound() {
        ProductNotFoundException ex = assertThrows(ProductNotFoundException.class,
                () -> productService.getProductById(999999L));
        assertEquals("Product not found with id: 999999", ex.getMessage());
    }
}
