package org.ecom.productcatalog.model;

import org.ecom.productcatalog.Product;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryAndProductModelTest {

    @Test
    void categoryAllArgsConstructor_populatesEveryField() {
        Product phone = new Product();
        phone.setName("Phone");
        List<Product> products = List.of(phone);

        Category category = new Category(7L, "Electronics", products);

        assertEquals(7L, category.getId());
        assertEquals("Electronics", category.getName());
        assertEquals(products, category.getProducts());
        assertEquals(1, category.getProducts().size());
        assertSame(phone, category.getProducts().get(0));
    }

    @Test
    void categoryAllArgsConstructor_acceptsNullsAndEmptyValues() {
        Category category = new Category(null, null, null);

        assertNull(category.getId());
        assertNull(category.getName());
        assertNull(category.getProducts());

        Category empty = new Category(0L, "", new ArrayList<>());

        assertEquals(0L, empty.getId());
        assertEquals("", empty.getName());
        assertTrue(empty.getProducts().isEmpty());
    }

    @Test
    void categoryNoArgsConstructor_leavesFieldsUnset() {
        Category category = new Category();

        assertNull(category.getId());
        assertNull(category.getName());
        assertNull(category.getProducts());
    }

    @Test
    void categorySetters_updateIdNameAndProducts() {
        Category category = new Category();

        category.setId(42L);
        category.setName("Books");
        List<Product> products = new ArrayList<>();
        Product novel = new Product();
        novel.setName("Novel");
        products.add(novel);
        category.setProducts(products);

        assertEquals(42L, category.getId());
        assertEquals("Books", category.getName());
        assertSame(products, category.getProducts());
        assertEquals("Novel", category.getProducts().get(0).getName());

        category.setId(null);
        category.setProducts(null);

        assertNull(category.getId());
        assertNull(category.getProducts());
    }

    @Test
    void productAllArgsConstructor_populatesEveryField() {
        Category category = new Category(3L, "Electronics", null);

        Product product = new Product(11L, "Phone", "A phone", "http://img/phone.png", 199.5, category);

        assertEquals(11L, product.getId());
        assertEquals("Phone", product.getName());
        assertEquals("A phone", product.getDescription());
        assertEquals("http://img/phone.png", product.getImageUrl());
        assertEquals(199.5, product.getPrice(), 0.0001);
        assertSame(category, product.getCategory());
        assertEquals("Electronics", product.getCategory().getName());
    }

    @Test
    void productAllArgsConstructor_acceptsNullsAndEdgeValues() {
        Product product = new Product(null, null, null, null, 0.0, null);

        assertNull(product.getId());
        assertNull(product.getName());
        assertNull(product.getDescription());
        assertNull(product.getImageUrl());
        assertEquals(0.0, product.getPrice(), 0.0001);
        assertNull(product.getCategory());

        Product negative = new Product(-1L, "", "", "", -9.99, null);

        assertEquals(-1L, negative.getId());
        assertEquals("", negative.getName());
        assertEquals(-9.99, negative.getPrice(), 0.0001);
    }

    @Test
    void productSetters_overrideConstructorValues() {
        Category first = new Category(1L, "First", null);
        Category second = new Category(2L, "Second", null);
        Product product = new Product(1L, "Old", "Old description", "http://img/old.png", 1.0, first);

        product.setId(2L);
        product.setName("New");
        product.setDescription("New description");
        product.setImageUrl("http://img/new.png");
        product.setPrice(2.5);
        product.setCategory(second);

        assertEquals(2L, product.getId());
        assertEquals("New", product.getName());
        assertEquals("New description", product.getDescription());
        assertEquals("http://img/new.png", product.getImageUrl());
        assertEquals(2.5, product.getPrice(), 0.0001);
        assertSame(second, product.getCategory());
    }
}
