package com.aristino.backend.dto.order;

import com.aristino.backend.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OrderItemResponse {
    private String productName;
    // Duong dan san pham (de khach bam sang trang san pham viet danh gia)
    private String productSlug;
    private String size;
    private String color;
    private BigDecimal price;
    private Integer quantity;

    public static OrderItemResponse from(OrderItem oi) {
        return new OrderItemResponse(
                oi.getProductName(),
                oi.getVariant().getProduct().getSlug(),
                oi.getVariant().getSize(),
                oi.getVariant().getColor(),
                oi.getPrice(),
                oi.getQuantity()
        );
    }
}