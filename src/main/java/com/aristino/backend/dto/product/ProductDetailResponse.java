package com.aristino.backend.dto.product;

import com.aristino.backend.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class ProductDetailResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String material;
    private BigDecimal basePrice;
    private String categoryName;
    private List<String> imageUrls;
    private List<ProductVariantResponse> variants;

    public static ProductDetailResponse from(Product p) {
        return new ProductDetailResponse(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                p.getMaterial(),
                p.getBasePrice(),
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getImages().stream().map(img -> img.getImageUrl()).toList(),
                p.getVariants().stream().map(ProductVariantResponse::from).toList()
        );
    }
}
