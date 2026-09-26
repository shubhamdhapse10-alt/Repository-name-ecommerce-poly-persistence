package com.example.ecommerce.controller;

import com.example.ecommerce.document.Product;
import com.example.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody Product product, @RequestParam(defaultValue = "0") int initialStock) {
        return productService.createProduct(product, initialStock);
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable String id) {
        return productService.getProduct(id);
    }

    @GetMapping
    public List<Product> list(@RequestParam(required = false) String category,
                               @RequestParam(required = false) String search) {
        if (category != null) return productService.getByCategory(category);
        if (search != null) return productService.searchByName(search);
        return productService.getAll();
    }
}
