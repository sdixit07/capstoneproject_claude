package org.ecom.productcatalog.controller;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.model.Category;
import org.ecom.productcatalog.repository.CategoryRepository;
import org.ecom.productcatalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    Long catId;
    Long clothingId;

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category cat = new Category();
        cat.setName("Electronics");
        cat = categoryRepository.save(cat);
        catId = cat.getId();

        Category clothing = new Category();
        clothing.setName("Clothing");
        clothing = categoryRepository.save(clothing);
        clothingId = clothing.getId();

        // 25 electronics priced 101.0 up to 125.0
        for (int i = 1; i <= 25; i++) {
            Product p = new Product();
            p.setName("iPhone " + i);
            p.setDescription("iPhone desc");
            p.setPrice(100.0 + i);
            p.setCategory(cat);
            productRepository.save(p);
        }

        // two cheap clothing items so the price buckets are distinguishable
        Product socks = new Product();
        socks.setName("Socks");
        socks.setDescription("Warm socks");
        socks.setPrice(19.99);
        socks.setCategory(clothing);
        productRepository.save(socks);

        Product jacket = new Product();
        jacket.setName("Winter jacket");
        jacket.setDescription("Light weight jacket");
        jacket.setPrice(39.99);
        jacket.setCategory(clothing);
        productRepository.save(jacket);
    }

    @Test
    void getProducts_noParams_returnsWholeCatalog() throws Exception {
        mockMvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(27)));
    }

    @Test
    void getProducts_search_filter_works() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("search", "iPhone 2")
                        .param("categoryId", String.valueOf(catId))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", containsStringIgnoringCase("iphone")))
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void getProducts_sort_price_desc_works() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("sort", "price,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price", greaterThan(120.0)));
    }

    @Test
    void getProducts_minPriceOnly_filtersOutCheaperProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "120")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[*].price", everyItem(greaterThanOrEqualTo(120.0))));
    }

    @Test
    void getProducts_maxPriceOnly_filtersOutExpensiveProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("maxPrice", "39.99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Socks", "Winter jacket")));
    }

    @Test
    void getProducts_bothBounds_returnInclusiveRange() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "110")
                        .param("maxPrice", "115")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[*].price", everyItem(allOf(
                        greaterThanOrEqualTo(110.0), lessThanOrEqualTo(115.0)))));
    }

    @Test
    void getProducts_boundsAreInclusive() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "125")
                        .param("maxPrice", "125")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].price").value(125.0));
    }

    @Test
    void getProducts_priceRangeWithoutMatches_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "5000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getProducts_priceRangeCombinesWithCategory() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("categoryId", String.valueOf(clothingId))
                        .param("minPrice", "30")
                        .param("maxPrice", "50")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Winter jacket"));
    }

    @Test
    void getProducts_minPriceGreaterThanMaxPrice_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "500")
                        .param("maxPrice", "100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/products"))
                .andExpect(jsonPath("$.message",
                        containsString("minPrice (500.0) must be less than or equal to maxPrice (100.0)")));
    }

    @Test
    void getProducts_negativePrice_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "-10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("minPrice must not be negative")));
    }

    @Test
    void getProducts_nonNumericPrice_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("maxPrice", "cheap")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("maxPrice")));
    }

    @Test
    void getProductsByCategoryPath_stillWorks() throws Exception {
        mockMvc.perform(get("/api/products/category/{categoryId}", clothingId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
