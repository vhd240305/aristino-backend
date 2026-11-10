package com.aristino.backend.service;

import com.aristino.backend.dto.product.ProductFilterOptionsResponse;
import com.aristino.backend.entity.Product;
import com.aristino.backend.entity.ProductVariant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Tính các lựa chọn của bộ lọc (size, màu, khoảng giá) từ dữ liệu sản phẩm đang bán
@Service
@Transactional(readOnly = true)
public class ProductFilterService {

    @PersistenceContext
    private EntityManager em;

    public ProductFilterOptionsResponse getOptions(Long categoryId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Giá thấp nhất / cao nhất
        CriteriaQuery<Tuple> priceQuery = cb.createTupleQuery();
        Root<Product> p = priceQuery.from(Product.class);
        priceQuery.multiselect(cb.min(p.<BigDecimal>get("basePrice")), cb.max(p.<BigDecimal>get("basePrice")));
        List<Predicate> priceWhere = new ArrayList<>();
        priceWhere.add(cb.equal(p.get("status"), Product.Status.ACTIVE));
        if (categoryId != null) priceWhere.add(cb.equal(p.get("category").get("id"), categoryId));
        priceQuery.where(priceWhere.toArray(new Predicate[0]));
        Tuple range = em.createQuery(priceQuery).getSingleResult();

        return new ProductFilterOptionsResponse(
                distinctVariantValues("size", categoryId),
                distinctVariantValues("color", categoryId),
                range.get(0, BigDecimal.class),
                range.get(1, BigDecimal.class)
        );
    }

    // Danh sách giá trị khác nhau của một thuộc tính biến thể ("size" hoặc "color")
    private List<String> distinctVariantValues(String attribute, Long categoryId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<ProductVariant> variant = query.from(ProductVariant.class);
        Join<ProductVariant, Product> product = variant.join("product");

        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(product.get("status"), Product.Status.ACTIVE));
        if (categoryId != null) where.add(cb.equal(product.get("category").get("id"), categoryId));

        query.select(variant.<String>get(attribute)).distinct(true).where(where.toArray(new Predicate[0]));
        return em.createQuery(query).getResultList().stream().filter(Objects::nonNull).toList();
    }
}
