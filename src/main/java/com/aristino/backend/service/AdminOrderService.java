package com.aristino.backend.service;

import com.aristino.backend.dto.admin.AdminCancelOrderRequest;
import com.aristino.backend.dto.admin.AdminOrderDetail;
import com.aristino.backend.dto.admin.AdminOrderSummary;
import com.aristino.backend.dto.order.OrderResponse;
import com.aristino.backend.entity.Order;
import com.aristino.backend.entity.User;
import com.aristino.backend.repository.OrderRepository;
import com.aristino.backend.spec.OrderSpecs;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quan ly don hang danh cho cua hang (admin).
 *
 * Luong trang thai: PENDING -> CONFIRMED -> SHIPPING -> COMPLETED
 * - Don online chua thanh toan thi chua duoc xac nhan.
 * - Giao thanh cong (COMPLETED) voi don COD: ghi nhan da thu tien (paymentStatus = PAID).
 * - Huy don: duoc khi PENDING hoac CONFIRMED (don da PAID thi hoan tien gia lap, cong lai kho).
 */
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderCancellationService cancellationService;
    private final OrderPolicy orderPolicy;

    @Transactional(readOnly = true)
    public Page<AdminOrderSummary> list(String status, String keyword, Pageable pageable) {
        Specification<Order> spec = Specification
                .where(OrderSpecs.hasStatus(parseStatus(status)))
                .and(OrderSpecs.matches(keyword));
        return orderRepository.findAll(spec, pageable).map(AdminOrderSummary::from);
    }

    @Transactional(readOnly = true)
    public AdminOrderDetail getDetail(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang"));
        return toDetail(order);
    }

    @Transactional
    public AdminOrderDetail updateStatus(Long id, String targetName) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang"));

        Order.OrderStatus target = Order.OrderStatus.valueOf(targetName);
        if (nextStatusOf(order.getOrderStatus()) != target) {
            throw new IllegalStateException("Khong the chuyen don tu trang thai hien tai sang trang thai nay");
        }
        if (target == Order.OrderStatus.CONFIRMED && isAwaitingPayment(order)) {
            throw new IllegalStateException("Don thanh toan online chua duoc thanh toan, chua the xac nhan");
        }

        // Giao thanh cong don COD: nhan vien giao hang da thu tien
        if (target == Order.OrderStatus.COMPLETED && order.getPaymentMethod() == Order.PaymentMethod.COD) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        }

        order.setOrderStatus(target);
        return toDetail(orderRepository.save(order));
    }

    @Transactional
    public AdminOrderDetail cancel(Long id, AdminCancelOrderRequest request) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang"));

        if (!canAdminCancel(order)) {
            throw new IllegalStateException("Chi huy duoc don dang cho xac nhan hoac da xac nhan");
        }
        String note = request.getNote() == null ? "" : request.getNote().trim();
        if ("SHOP_OTHER".equals(request.getReason()) && note.isEmpty()) {
            throw new IllegalArgumentException("Vui long nhap ly do huy don");
        }

        // Dung chung logic voi khach huy / het han: cong lai kho, hoan tien neu da thanh toan
        cancellationService.cancel(order, request.getReason(), note.isEmpty() ? null : note);
        return toDetail(order);
    }

    // ===== Ham phu =====

    private AdminOrderDetail toDetail(Order order) {
        User customer = order.getUser();
        Order.OrderStatus next = nextStatusOf(order.getOrderStatus());
        boolean blocked = next == Order.OrderStatus.CONFIRMED && isAwaitingPayment(order);

        return new AdminOrderDetail(
                OrderResponse.from(order, orderPolicy),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                next == null || blocked ? List.of() : List.of(next.name()),
                blocked ? "AWAITING_PAYMENT" : null,
                canAdminCancel(order)
        );
    }

    // Buoc tiep theo hop le cua don; null neu don da o trang thai cuoi / bi huy
    private Order.OrderStatus nextStatusOf(Order.OrderStatus current) {
        return switch (current) {
            case PENDING -> Order.OrderStatus.CONFIRMED;
            case CONFIRMED -> Order.OrderStatus.SHIPPING;
            case SHIPPING -> Order.OrderStatus.COMPLETED;
            default -> null;
        };
    }

    private boolean isAwaitingPayment(Order order) {
        return order.getPaymentMethod() != Order.PaymentMethod.COD
                && order.getPaymentStatus() != Order.PaymentStatus.PAID;
    }

    private boolean canAdminCancel(Order order) {
        return order.getOrderStatus() == Order.OrderStatus.PENDING
                || order.getOrderStatus() == Order.OrderStatus.CONFIRMED;
    }

    private Order.OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Order.OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Trang thai don hang khong hop le");
        }
    }
}