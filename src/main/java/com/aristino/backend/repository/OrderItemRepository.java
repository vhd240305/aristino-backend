package com.aristino.backend.repository;

import com.aristino.backend.entity.Order;
import com.aristino.backend.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    // Quan ly san pham: bien the / san pham da co don hang thi khong duoc xoa
    boolean existsByVariantId(Long variantId);

    boolean existsByVariantProductId(Long productId);

    // Danh gia san pham: cac dong don hang cua khach da mua san pham nay trong don o trang thai `status` (moi nhat truoc)
    @Query("select oi from OrderItem oi join oi.order o "
            + "where o.user.id = :userId and o.orderStatus = :status and oi.variant.product.id = :productId "
            + "order by o.createdAt desc")
    List<OrderItem> findPurchasedItems(@Param("userId") Long userId,
                                       @Param("productId") Long productId,
                                       @Param("status") Order.OrderStatus status,
                                       Pageable pageable);
}