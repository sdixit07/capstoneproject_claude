package org.ecom.productcatalog.controller;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.dto.PagedResponse;
import org.ecom.productcatalog.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private static final int MAX_SIZE = 100;

    @Autowired
    public ProductService productService;

    @GetMapping
    public PagedResponse<Product> getAllProducts(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size){
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and " + MAX_SIZE);
        }
        // Offset (page * size) would overflow int inside Spring Data: answer with an empty page instead of a 500.
        if ((long) page * size > Integer.MAX_VALUE) {
            long total = productService.countProducts();
            int totalPages = (int) ((total + size - 1) / size);
            return new PagedResponse<>(List.of(), page, size, total, totalPages);
        }
        Page<Product> result = productService.getProducts(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        return PagedResponse.from(result);
    }

    @GetMapping("category/{categoryId}")
    public List<Product> getProductByCategory(@PathVariable Long categoryId){
        return productService.getProductByCategory(categoryId);
    }
}
