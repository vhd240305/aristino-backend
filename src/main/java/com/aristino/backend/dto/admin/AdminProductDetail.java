package com.aristino.backend.dto.admin;

import com.aristino.backend.entity.Product;
import com.aristino.backend.entity.ProductImage;
import com.aristino.backend.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Chi tiet san pham de do vao form sua o trang quan tri. */
public record AdminProductDetail(
        Long id,
        String name,
        String slug,
        Long categoryId,
        String description,
        String material,
        BigDecimal basePrice,
        String status,
        List<Image> images,
        List<Variant> variants
) {
    public record Image(Long id, String url, boolean thumbnail, int sortOrder) {
    }

    public record Variant(Long id, String size, String color, String sku, BigDecimal price, int stockQuantity) {
    }

    public static AdminProductDetail from(Product p) {
        List<Image> images = p.getImages().stream()
                .sorted(Comparator.comparingInt((ProductImage i) -> i.getSortOrder() == null ? 0 : i.getSortOrder()))
                .map(i -> new Image(i.getId(), i.getImageUrl(), Boolean.TRUE.equals(i.getIsThumbnail()),
                        i.getSortOrder() == null ? 0 : i.getSortOrder()))
                .toList();

        List<Variant> variants = p.getVariants().stream()
                .sorted(Comparator.comparing(ProductVariant::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(v -> new Variant(v.getId(), v.getSize(), v.getColor(), v.getSku(), v.getPrice(), v.getStockQuantity()))
                .toList();

        return new AdminProductDetail(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getCategory().getId(),
                p.getDescription(),
                p.getMaterial(),
                p.getBasePrice(),
                p.getStatus().name(),
                images,
                variants
        );
    }
}