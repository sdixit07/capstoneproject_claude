package org.ecom.productcatalog.service;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductService service = new ProductService();

    ProductServiceTest() {
        service.productRepository = repository;
    }

    @Test
    void getProductById_existing_returnsProduct() {
        Product p = new Product();
        p.setId(5L);
        p.setName("Phone");
        when(repository.findById(5L)).thenReturn(Optional.of(p));

        assertSame(p, service.getProductById(5L));
        verify(repository, times(1)).findById(5L);
    }

    @Test
    void getProductById_missing_throwsNotFound() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.getProductById(42L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Product not found with id 42", ex.getReason());
        verify(repository, times(1)).findById(42L);
    }

    @Test
    void getProductById_zeroAndNegative_throwNotFound() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> service.getProductById(0L)).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> service.getProductById(-1L)).getStatusCode());
    }
}
