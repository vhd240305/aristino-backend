package com.aristino.backend.controller;

import com.aristino.backend.dto.category.CategoryResponse;
import com.aristino.backend.dto.product.ProductSummaryResponse;
import com.aristino.backend.service.CategoryService;
import com.aristino.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    // GET /api/categories
    @GetMapping
    public List<CategoryResponse> getAll() {
        return categoryService.getAll();
    }

    // GET /api/categories/{id}/products?page=0&size=12
    // Khop voi categoryService.getByCategory ben frontend (productService.js)
    @GetMapping("/{id}/products")
    public Page<ProductSummaryResponse> getProductsByCategory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.getByCategory(id, pageable);
    }
}
