package org.ecom.productcatalog.config;

import org.ecom.productcatalog.repository.CategoryRepository;
import org.ecom.productcatalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void run_clearsAndSeedsData() throws Exception {
        dataSeeder.run();

        verify(productRepository).deleteAll();
        verify(categoryRepository).deleteAll();
        verify(categoryRepository).saveAll(anyList());
        verify(productRepository).saveAll(anyList());
    }
}
