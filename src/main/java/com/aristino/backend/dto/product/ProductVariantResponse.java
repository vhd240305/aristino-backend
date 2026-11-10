package com.aristino.backend.dto.product;

import com.aristino.backend.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ProductVariantResponse {
    private Long id;
    private String size;
    private String color;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;

    public static ProductVariantResponse from(ProductVariant v) {
        return new ProductVariantResponse(
                v.getId(), v.getSize(), v.getColor(), v.getSku(),
                v.getPrice(), v.getStockQuantity()
        );
    }
}
