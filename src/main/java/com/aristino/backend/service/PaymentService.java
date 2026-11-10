package com.aristino.backend.service;

import com.aristino.backend.common.CurrentUserProvider;
import com.aristino.backend.dto.order.OrderResponse;
import com.aristino.backend.dto.payment.PaymentResultRequest;
import com.aristino.backend.entity.Order;
import com.aristino.backend.entity.User;
import com.aristino.backend.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * THANH TOAN GIA LAP (VNPay / MoMo) - chi dung cho do an.
 *
 * Thuc te: cong thanh toan goi ve server cua minh (IPN/callback) kem chu ky bao mat de xac nhan ket qua.
 * O day: trang "cong thanh toan gia lap" tren frontend goi API nay de bao ket qua thanh cong/that bai.
 * Vi ai cung goi duoc API nay cho don cua chinh minh, KHONG dung cach nay khi chay that.
 * Tat tinh nang bang cach dat app.payment.mock-enabled=false trong application.yml.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OrderPolicy orderPolicy;
    private final OrderCancellationService cancellationService;

    @Value("${app.payment.mock-enabled:true}")
    private boolean mockEnabled;

    @Transactional
    public OrderResponse confirmMockPayment(Long orderId, PaymentResultRequest request) {
        if (!mockEnabled) {
            throw new IllegalStateException("Cong thanh toan gia lap dang tat");
        }

        User user = currentUserProvider.getCurrentUser();

        // Khoa dong don de khong bi huy / thanh toan chong cheo
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Ban khong co quyen thanh toan don hang nay");
        }
        if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
            throw new IllegalStateException("Don hang thanh toan khi nhan hang, khong can thanh toan online");
        }
        if (order.getOrderStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Don hang da bi huy, khong the thanh toan");
        }
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new IllegalStateException("Don hang nay da duoc thanh toan");
        }
        if (order.getOrderStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException("Don hang khong con o trang thai cho thanh toan");
        }

        // Qua han thanh toan: khong nhan thanh toan nua, huy don + nha kho ngay.
        // Tra ve don (da o trang thai CANCELLED) de frontend hien thong bao het han.
        if (orderPolicy.isPaymentExpired(order)) {
            cancellationService.cancel(order, "PAYMENT_EXPIRED", null);
            return OrderResponse.from(order, orderPolicy);
        }

        boolean success = "SUCCESS".equals(request.getResult());
        order.setPaymentStatus(success ? Order.PaymentStatus.PAID : Order.PaymentStatus.FAILED);

        // That bai: giu nguyen don (van PENDING) de khach thanh toan lai trong thoi han
        return OrderResponse.from(orderRepository.save(order), orderPolicy);
    }
}