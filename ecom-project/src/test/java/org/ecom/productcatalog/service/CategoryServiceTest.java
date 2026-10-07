package org.ecom.productcatalog.service;

import org.ecom.productcatalog.model.Category;
import org.ecom.productcatalog.repository.CategoryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final CategoryService service = new CategoryService();

    CategoryServiceTest() {
        service.categoryRepository = repository;
    }

    @Test
    void getAllCategories_returnsRepositoryContent() {
        Category electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        Category books = new Category();
        books.setId(2L);
        books.setName("Books");
        when(repository.findAll()).thenReturn(List.of(electronics, books));

        List<Category> result = service.getAllCategories();

        assertEquals(2, result.size());
        assertSame(electronics, result.get(0));
        assertEquals("Books", result.get(1).getName());
        verify(repository, times(1)).findAll();
    }

    @Test
    void getAllCategories_emptyRepository_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<Category> result = service.getAllCategories();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findAll();
    }
}
