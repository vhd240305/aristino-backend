package com.aristino.backend.controller;

import com.aristino.backend.dto.product.ProductDetailResponse;
import com.aristino.backend.dto.product.ProductFilterOptionsResponse;
import com.aristino.backend.dto.product.ProductSummaryResponse;
import com.aristino.backend.service.ProductFilterService;
import com.aristino.backend.service.ProductSearchCriteria;
import com.aristino.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductFilterService productFilterService;

    // GET /api/products?q=ao&category=1&minPrice=100000&maxPrice=500000&sizes=M,L&colors=Trắng&sort=price_asc&page=0&size=12
    // Mọi tham số đều không bắt buộc. sort: newest (mặc định) | price_asc | price_desc | name_asc
    @GetMapping
    public Page<ProductSummaryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sizes,
            @RequestParam(required = false) String colors,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        ProductSearchCriteria criteria = new ProductSearchCriteria(
                q, category, minPrice, maxPrice, splitCsv(sizes), splitCsv(colors), sort);
        return productService.search(criteria, Math.max(0, page), Math.min(Math.max(1, size), 100));
    }

    // GET /api/products/filters?category=1 : danh sách size, màu và khoảng giá để dựng bộ lọc
    @GetMapping("/filters")
    public ProductFilterOptionsResponse filters(@RequestParam(required = false) Long category) {
        return productFilterService.getOptions(category);
    }

    // GET /api/products/{slug}
    @GetMapping("/{slug}")
    public ProductDetailResponse getBySlug(@PathVariable String slug) {
        return productService.getBySlug(slug);
    }

    // "M, L ,XL" -> ["M","L","XL"]
    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
