package com.aristino.backend.repository;

import com.aristino.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// JpaSpecificationExecutor: cho phep tim kiem nhieu dieu kien ket hop (xem ProductSpecs)
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByStatus(Product.Status status, Pageable pageable);

    // Dung khi quan ly danh muc: dem / kiem tra san pham thuoc danh muc
    long countByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);

    // Chatbot: lay mot lan toan bo san pham theo trang thai kem danh muc va bien the (tranh truy van lap)
    @Query("select distinct p from Product p join fetch p.category left join fetch p.variants where p.status = :status")
    List<Product> findAllWithVariantsByStatus(@Param("status") Product.Status status);
}