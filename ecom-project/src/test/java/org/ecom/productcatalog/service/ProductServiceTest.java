package org.ecom.productcatalog.service;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private final ProductRepository repo = mock(ProductRepository.class);
    private final ProductService service = new ProductService();

    ProductServiceTest() {
        service.productRepository = repo;
    }

    @Test
    void getProductsDelegatesToFindAllPageable() {
        Pageable pageable = PageRequest.of(1, 5);
        Page<Product> page = new PageImpl<>(List.of(new Product()), pageable, 6);
        when(repo.findAll(pageable)).thenReturn(page);
        assertSame(page, service.getProducts(pageable));
        verify(repo).findAll(pageable);
    }

    @Test
    void countProductsDelegatesToCount() {
        when(repo.count()).thenReturn(42L);
        assertEquals(42L, service.countProducts());
    }

    @Test
    void getAllProductsStillReturnsList() {
        List<Product> all = List.of(new Product(), new Product());
        when(repo.findAll()).thenReturn(all);
        assertEquals(all, service.getAllProducts());
    }

    @Test
    void getProductByCategoryDelegates() {
        List<Product> list = List.of(new Product());
        when(repo.findByCategoryId(3L)).thenReturn(list);
        assertEquals(list, service.getProductByCategory(3L));
    }
}
