package com.aristino.backend.service;

import com.aristino.backend.entity.Order;
import com.aristino.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tac vu chay dinh ky: tu huy cac don online qua han thanh toan va nha lai hang vao kho.
 * Can @EnableScheduling o AristinoBackendApplication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationJob {

    private final OrderRepository orderRepository;
    private final OrderCancellationService cancellationService;
    private final OrderPolicy orderPolicy;

    // Chay 1 phut/lan (tinh tu luc lan chay truoc ket thuc), lan dau sau khi khoi dong 30 giay
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void cancelExpiredOrders() {
        List<Long> ids = orderRepository.findExpiredUnpaidIds(
                Order.OrderStatus.PENDING,
                Order.PaymentMethod.COD,
                List.of(Order.PaymentStatus.PENDING, Order.PaymentStatus.FAILED),
                orderPolicy.expiryCutoff());

        for (Long id : ids) {
            try {
                if (cancellationService.expireIfUnpaid(id)) {
                    log.info("Da tu dong huy don {} do qua han thanh toan", id);
                }
            } catch (Exception ex) {
                // Vi du xung dot ton kho: bo qua, lan quet sau se thu lai
                log.warn("Khong the tu dong huy don {}: {}", id, ex.getMessage());
            }
        }
    }
}