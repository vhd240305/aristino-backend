package com.aristino.backend.service;

import java.math.BigDecimal;
import java.util.List;

// Gom các điều kiện tìm kiếm/lọc sản phẩm. Giá trị null hoặc danh sách rỗng nghĩa là không lọc theo điều kiện đó.
public record ProductSearchCriteria(
        String q,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<String> sizes,
        List<String> colors,
        String sort
) {
    public static ProductSearchCriteria ofCategory(Long categoryId) {
        return new ProductSearchCriteria(null, categoryId, null, null, List.of(), List.of(), "newest");
    }
}
