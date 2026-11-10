package com.aristino.backend.repository;

import com.aristino.backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    // Dung de kiem tra bien the da co trong gio chua, neu co thi cong don quantity thay vi tao dong moi
    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);

    // Khi cua hang go bo mot bien the: xoa no khoi gio hang cua moi khach
    @Modifying(flushAutomatically = true)
    @Query("delete from CartItem ci where ci.variant.id = :variantId")
    int deleteByVariantId(@Param("variantId") Long variantId);

    // Khi cua hang xoa san pham: xoa moi bien the cua san pham do khoi gio hang
    @Modifying(flushAutomatically = true)
    @Query("delete from CartItem ci where ci.variant.id in (select v.id from ProductVariant v where v.product.id = :productId)")
    int deleteByProductId(@Param("productId") Long productId);
}