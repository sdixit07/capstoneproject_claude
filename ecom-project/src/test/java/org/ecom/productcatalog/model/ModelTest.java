package org.ecom.productcatalog.model;

import org.ecom.productcatalog.Product;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModelTest {

    @Test
    void productSettersAndGetters() {
        Category category = new Category();
        Product p = new Product();
        assertNull(p.getId());
        p.setId(1L);
        p.setName("Laptop");
        p.setDescription("desc");
        p.setImageUrl("http://img");
        p.setPrice(9.5);
        p.setCategory(category);

        assertEquals(1L, p.getId());
        assertEquals("Laptop", p.getName());
        assertEquals("desc", p.getDescription());
        assertEquals("http://img", p.getImageUrl());
        assertEquals(9.5, p.getPrice());
        assertSame(category, p.getCategory());
    }

    @Test
    void productAllArgsConstructor() {
        Category category = new Category();
        Product p = new Product(2L, "n", "d", "i", 1.0, category);
        assertEquals(2L, p.getId());
        assertEquals("n", p.getName());
        assertEquals("d", p.getDescription());
        assertEquals("i", p.getImageUrl());
        assertEquals(1.0, p.getPrice());
        assertSame(category, p.getCategory());
    }

    @Test
    void categorySettersAndGetters() {
        Category c = new Category();
        List<Product> products = List.of(new Product());
        c.setId(3L);
        c.setName("Clothing");
        c.setProducts(products);

        assertEquals(3L, c.getId());
        assertEquals("Clothing", c.getName());
        assertSame(products, c.getProducts());
    }

    @Test
    void categoryAllArgsConstructor() {
        List<Product> products = List.of();
        Category c = new Category(4L, "Home", products);
        assertEquals(4L, c.getId());
        assertEquals("Home", c.getName());
        assertSame(products, c.getProducts());
    }
}
