package com.aristino.backend.service;

import com.aristino.backend.common.CurrentUserProvider;
import com.aristino.backend.dto.order.CreateOrderRequest;
import com.aristino.backend.dto.order.OrderResponse;
import com.aristino.backend.entity.*;
import com.aristino.backend.exception.InsufficientStockException;
import com.aristino.backend.repository.CartItemRepository;
import com.aristino.backend.repository.CartRepository;
import com.aristino.backend.repository.OrderRepository;
import com.aristino.backend.repository.ProductVariantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderRepository orderRepository;
    private final AddressService addressService;
    private final CurrentUserProvider currentUserProvider;
    private final OrderPolicy orderPolicy;

    // @Transactional o day rat quan trong: neu 1 bien the het hang giua chung,
    // toan bo don hang se ROLLBACK, khong tao don thieu san pham hoac tru kho sai
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User user = currentUserProvider.getCurrentUser();

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay gio hang"));

        List<CartItem> cartItems = new ArrayList<>(cart.getItems());
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Gio hang dang trong, khong the dat hang");
        }

        // getOwnedAddressOrThrow dam bao dia chi nay thuoc dung user dang dat hang
        Address address = addressService.getOwnedAddressOrThrow(request.getAddressId());

        Order.PaymentMethod paymentMethod = parsePaymentMethod(request.getPaymentMethod());

        Order order = Order.builder()
                .user(user)
                .orderCode(generateOrderCode())
                .address(address)
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getPhone())
                .shippingProvince(address.getProvince())
                .shippingDistrict(address.getDistrict())
                .shippingWard(address.getWard())
                .shippingDetailAddress(address.getDetailAddress())
                .shippingFee(BigDecimal.ZERO) // mien phi van chuyen toan bo don hang
                .paymentMethod(paymentMethod)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO) // se tinh lai ben duoi
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getVariant();

            // San pham da bi cua hang an: khong cho dat, khach can xoa khoi gio hang
            if (variant.getProduct().getStatus() == Product.Status.HIDDEN) {
                throw new IllegalStateException("San pham \"" + variant.getProduct().getName()
                        + "\" hien khong con ban, vui long xoa khoi gio hang");
            }

            // Kiem tra lai ton kho ngay truoc khi tru - phong truong hop hang da het
            // tu luc khach them vao gio den luc bam dat hang
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "San pham \"" + variant.getProduct().getName() + "\" (" +
                        variant.getSize() + "/" + variant.getColor() + ") khong du ton kho"
                );
            }

            // Tru ton kho - cot "version" (@Version trong entity) se duoc Hibernate
            // tu kiem tra khi save(): neu co nguoi khac cung sua bien the nay giua chung,
            // se nem ObjectOptimisticLockingFailureException thay vi luu de len nhau
            variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
            variantRepository.save(variant);

            OrderItem orderItem = OrderItem.builder()
                    .variant(variant)
                    .productName(variant.getProduct().getName())
                    .price(variant.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();
            orderItem.setOrder(order);
            order.getItems().add(orderItem);

            totalAmount = totalAmount.add(variant.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setTotalAmount(totalAmount.add(order.getShippingFee()));

        Order saved = orderRepository.save(order);

        // Dat hang xong thi xoa het cac dong trong gio (khong xoa cai Cart, chi xoa cart_items)
        cart.getItems().clear(); // orphanRemoval = true nên Hibernate tự xóa các dòng khỏi DB khi commit

        return OrderResponse.from(saved, orderPolicy);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        User user = currentUserProvider.getCurrentUser();
        return orderRepository.findByUserId(user.getId(), pageable)
                .map(order -> OrderResponse.from(order, orderPolicy));
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId) {
        User user = currentUserProvider.getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Ban khong co quyen xem don hang nay");
        }
        return OrderResponse.from(order, orderPolicy);
    }

    private Order.PaymentMethod parsePaymentMethod(String raw) {
        try {
            return Order.PaymentMethod.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Hinh thuc thanh toan khong hop le: " + raw);
        }
    }

    // Sinh ma don hang dang: DH + yyyyMMddHHmmss + 3 so ngau nhien
    private String generateOrderCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomSuffix = new Random().nextInt(900) + 100;
        return "DH" + timestamp + randomSuffix;
    }
}