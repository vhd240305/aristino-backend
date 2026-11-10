package com.aristino.backend.dto.product;

import com.aristino.backend.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

// Dung cho trang danh sach san pham - chi can thong tin toi thieu, khong can day du variants/images
@Getter
@AllArgsConstructor
public class ProductSummaryResponse {
    private Long id;
    private String name;
    private String slug;
    private BigDecimal basePrice;
    private String thumbnailUrl;

    public static ProductSummaryResponse from(Product p) {
        String thumbnail = p.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsThumbnail()))
                .findFirst()
                .or(() -> p.getImages().stream().findFirst())
                .map(img -> img.getImageUrl())
                .orElse(null);

        return new ProductSummaryResponse(
                p.getId(), p.getName(), p.getSlug(), p.getBasePrice(), thumbnail
        );
    }
}
