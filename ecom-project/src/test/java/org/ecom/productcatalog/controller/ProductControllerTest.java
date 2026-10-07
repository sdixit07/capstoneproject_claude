package org.ecom.productcatalog.controller;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private Product product(String name) {
        Product p = new Product();
        p.setName(name);
        return p;
    }

    @Test
    void getAllProducts_returnsJsonList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(product("A"), product("B")));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("A"));
    }

    @Test
    void getProductByCategory_returnsJsonList() throws Exception {
        when(productService.getProductByCategory(3L)).thenReturn(List.of(product("C")));

        mockMvc.perform(get("/api/products/category/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("C"));
    }

    @Test
    void getProductByCategory_emptyList() throws Exception {
        when(productService.getProductByCategory(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/products/category/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getProductByCategory_nonNumericId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/category/abc"))
                .andExpect(status().isBadRequest());
    }
}
