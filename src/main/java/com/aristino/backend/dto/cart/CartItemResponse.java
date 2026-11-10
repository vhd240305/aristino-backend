package com.aristino.backend.dto.cart;

import com.aristino.backend.entity.CartItem;
import com.aristino.backend.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long variantId;
    private String productName;
    private String size;
    private String color;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
    private BigDecimal lineTotal;

    public static CartItemResponse from(CartItem item) {
        Product product = item.getVariant().getProduct();
        String imageUrl = product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsThumbnail()))
                .findFirst()
                .or(() -> product.getImages().stream().findFirst())
                .map(img -> img.getImageUrl())
                .orElse(null);

        BigDecimal lineTotal = item.getVariant().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return new CartItemResponse(
                item.getId(),
                item.getVariant().getId(),
                product.getName(),
                item.getVariant().getSize(),
                item.getVariant().getColor(),
                item.getVariant().getPrice(),
                item.getQuantity(),
                imageUrl,
                lineTotal
        );
    }
}