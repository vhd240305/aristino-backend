package com.aristino.backend.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

// Các lựa chọn để dựng bộ lọc trên giao diện: size, màu và khoảng giá hiện có
@Getter
@AllArgsConstructor
public class ProductFilterOptionsResponse {
    private List<String> sizes;
    private List<String> colors;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
