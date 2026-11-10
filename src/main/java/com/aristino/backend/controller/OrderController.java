package com.aristino.backend.controller;

import com.aristino.backend.dto.order.CancelOrderRequest;
import com.aristino.backend.dto.order.CreateOrderRequest;
import com.aristino.backend.dto.order.OrderResponse;
import com.aristino.backend.service.OrderCancellationService;
import com.aristino.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderCancellationService cancellationService;

    // POST /api/orders - tao don hang tu gio hang hien tai
    @PostMapping
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    // GET /api/orders/me?page=0&size=10 - lich su don hang, moi nhat truoc
    @GetMapping("/me")
    public Page<OrderResponse> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderService.getMyOrders(pageable);
    }

    // GET /api/orders/{id} - xem chi tiet 1 don hang
    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    // PUT /api/orders/{id}/cancel - khach huy don (chi khi cua hang chua xac nhan)
    @PutMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelOrderRequest request) {
        return cancellationService.cancelByCustomer(id, request);
    }
}