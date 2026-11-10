package com.aristino.backend.spec;

import com.aristino.backend.entity.Product;
import com.aristino.backend.entity.ProductVariant;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Từng điều kiện lọc sản phẩm. Điều kiện nào không có giá trị thì trả về null (bỏ qua, không lọc).
public final class ProductSpecs {

    private ProductSpecs() {
    }

    // Chỉ lấy sản phẩm đang bán
    public static Specification<Product> active() {
        return (root, query, cb) -> cb.equal(root.get("status"), Product.Status.ACTIVE);
    }

    // Tên chứa từ khóa. Cơ sở dữ liệu dùng collation utf8mb4_unicode_ci nên gõ "ao so mi" vẫn ra "Áo sơ mi"
    public static Specification<Product> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.<String>get("name")), pattern);
    }

    public static Specification<Product> inCategory(Long categoryId) {
        if (categoryId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> priceAtLeast(BigDecimal min) {
        if (min == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("basePrice"), min);
    }

    public static Specification<Product> priceAtMost(BigDecimal max) {
        if (max == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("basePrice"), max);
    }

    // Sản phẩm có ít nhất 1 biến thể CÒN HÀNG thỏa size và/hoặc màu được chọn
    // (chọn nhiều size/màu thì khớp một trong các giá trị đó)
    public static Specification<Product> hasStockedVariant(List<String> sizes, List<String> colors) {
        boolean noSize = sizes == null || sizes.isEmpty();
        boolean noColor = colors == null || colors.isEmpty();
        if (noSize && noColor) return null;

        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ProductVariant> variant = sub.from(ProductVariant.class);

            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(variant.get("product"), root));
            conditions.add(cb.greaterThan(variant.<Integer>get("stockQuantity"), 0));
            if (!noSize) conditions.add(variant.get("size").in(sizes));
            if (!noColor) conditions.add(variant.get("color").in(colors));

            sub.select(variant.<Long>get("id")).where(conditions.toArray(new Predicate[0]));
            return cb.exists(sub);
        };
    }
}
