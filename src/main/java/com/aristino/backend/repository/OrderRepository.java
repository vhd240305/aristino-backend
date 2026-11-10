package com.aristino.backend.repository;

import com.aristino.backend.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Optional<Order> findByOrderCode(String orderCode);

    // Khi xoa dia chi: bo tham chieu address_id trong cac don cu (don van giu ban chup dia chi)
    @Modifying(flushAutomatically = true)
    @Query("update Order o set o.address = null where o.address.id = :addressId")
    int detachAddress(@Param("addressId") Long addressId);

    // Khoa dong don hang (SELECT ... FOR UPDATE) de "thanh toan" va "huy don" khong chay chong cheo
    // Chi goi trong ham co @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    // Id cac don online dang cho thanh toan nhung da qua han (de tu dong huy)
    @Query("select o.id from Order o "
            + "where o.orderStatus = :status and o.paymentMethod <> :cod "
            + "and o.paymentStatus in :unpaid and o.createdAt < :cutoff")
    List<Long> findExpiredUnpaidIds(@Param("status") Order.OrderStatus status,
                                    @Param("cod") Order.PaymentMethod cod,
                                    @Param("unpaid") Collection<Order.PaymentStatus> unpaid,
                                    @Param("cutoff") LocalDateTime cutoff);
}