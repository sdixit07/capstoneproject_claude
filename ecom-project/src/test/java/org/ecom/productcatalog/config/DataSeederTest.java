package org.ecom.productcatalog.config;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataSeederTest {

    @Autowired
    private DataSeeder dataSeeder;
    @Autowired
    private ProductRepository productRepository;

    @Test
    void seedsAtLeast25ProductsAcrossThreeCategories() throws Exception {
        dataSeeder.run();
        List<Product> all = productRepository.findAll();
        assertTrue(all.size() >= 25, "expected >= 25 products but was " + all.size());
        Set<String> categories = all.stream().map(p -> p.getCategory().getName()).collect(Collectors.toSet());
        assertEquals(3, categories.size());
        for (String c : List.of("Electronics", "Clothing", "Home and Kitchen")) {
            assertTrue(categories.contains(c), "missing category " + c);
        }
    }

    @Test
    void seedingTwiceDoesNotDuplicate() throws Exception {
        dataSeeder.run();
        long first = productRepository.count();
        dataSeeder.run();
        assertEquals(first, productRepository.count());
    }
}
