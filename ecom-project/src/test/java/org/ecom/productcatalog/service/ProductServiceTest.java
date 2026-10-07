package org.ecom.productcatalog.service;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.exception.ProductNotFoundException;
import org.ecom.productcatalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProductById_returnsProductWhenFound() {
        Product product = new Product();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.getProductById(1L);

        assertSame(product, result);
        verify(productRepository).findById(1L);
    }

    @Test
    void getProductById_throwsWhenNotFound() {
        when(productRepository.findById(999999L)).thenReturn(Optional.empty());

        ProductNotFoundException ex = assertThrows(ProductNotFoundException.class,
                () -> productService.getProductById(999999L));

        assertTrue(ex.getMessage().contains("999999"));
        assertEquals("Product not found with id: 999999", ex.getMessage());
    }

    @Test
    void getProductById_throwsForZeroAndNegativeIds() {
        when(productRepository.findById(0L)).thenReturn(Optional.empty());
        when(productRepository.findById(-1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(0L));
        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(-1L));
    }
}
