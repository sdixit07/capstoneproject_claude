package org.ecom.productcatalog.service;

import org.ecom.productcatalog.model.Category;
import org.ecom.productcatalog.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getAllCategories_returnsRepositoryResult() {
        List<Category> categories = List.of(new Category(), new Category());
        when(categoryRepository.findAll()).thenReturn(categories);
        assertEquals(categories, categoryService.getAllCategories());
    }

    @Test
    void getAllCategories_emptyWhenNone() {
        when(categoryRepository.findAll()).thenReturn(List.of());
        assertTrue(categoryService.getAllCategories().isEmpty());
    }
}
