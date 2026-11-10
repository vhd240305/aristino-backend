package com.aristino.backend.service;

import com.aristino.backend.common.CurrentUserProvider;
import com.aristino.backend.dto.order.CancelOrderRequest;
import com.aristino.backend.dto.order.OrderResponse;
import com.aristino.backend.entity.Order;
import com.aristino.backend.entity.OrderItem;
import com.aristino.backend.entity.ProductVariant;
import com.aristino.backend.entity.User;
import com.aristino.backend.repository.OrderRepository;
import com.aristino.backend.repository.ProductVariantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Huy don hang. Moi truong hop huy (khach huy, het han thanh toan, sau nay la admin huy)
 * deu di qua ham cancel() de xu ly giong nhau: cong lai kho, hoan tien neu da thanh toan.
 */
@Service
@RequiredArgsConstructor
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OrderPolicy orderPolicy;

    // ===== Khach hang tu huy don =====
    @Transactional
    public OrderResponse cancelByCustomer(Long orderId, CancelOrderRequest request) {
        User user = currentUserProvider.getCurrentUser();

        // Khoa dong don de khong bi thanh toan / huy chong cheo
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Ban khong co quyen huy don hang nay");
        }
        if (order.getOrderStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Don hang nay da duoc huy truoc do");
        }
        if (!orderPolicy.canCustomerCancel(order)) {
            throw new IllegalStateException(
                    "Don hang da duoc cua hang xu ly, khong the tu huy. Vui long lien he cua hang de duoc ho tro");
        }

        String note = request.getNote() == null ? "" : request.getNote().trim();
        if ("OTHER".equals(request.getReason()) && note.isEmpty()) {
            throw new IllegalArgumentException("Vui long nhap ly do huy don");
        }

        cancel(order, request.getReason(), note.isEmpty() ? null : note);
        return OrderResponse.from(order, orderPolicy);
    }

    // ===== He thong tu huy don qua han thanh toan (goi tu OrderExpirationJob) =====
    // Moi don chay trong 1 giao dich rieng: 1 don loi khong lam hong cac don con lai
    @Transactional
    public boolean expireIfUnpaid(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        // Kiem tra lai sau khi khoa: co the khach vua thanh toan / vua huy xong
        if (order == null || !orderPolicy.isPaymentExpired(order)) {
            return false;
        }
        cancel(order, "PAYMENT_EXPIRED", null);
        return true;
    }

    /**
     * Xu ly chung khi huy don. PHAI goi trong mot giao dich (@Transactional) va dong don da duoc khoa.
     * - Cong lai ton kho cho tung san pham (cung co che @Version chong xung dot nhu luc tru kho)
     * - Don da thanh toan online -> chuyen thanh toan sang REFUNDED (hoan tien GIA LAP)
     * - Don chua tra tien (COD, online chua tra / that bai) -> khong phat sinh hoan tien
     */
    public void cancel(Order order, String reasonCode, String note) {
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            variantRepository.save(variant);
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
        }

        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        order.setCancelReason(reasonCode);
        order.setCancelNote(note);
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}