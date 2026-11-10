package com.aristino.backend.controller;

import com.aristino.backend.dto.cart.AddCartItemRequest;
import com.aristino.backend.dto.cart.CartResponse;
import com.aristino.backend.dto.cart.UpdateCartItemRequest;
import com.aristino.backend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // GET /api/cart - lay gio hang cua chinh user dang dang nhap
    @GetMapping
    public CartResponse getCart() {
        return cartService.getMyCart();
    }

    // POST /api/cart/items - than vao gio
    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(request);
    }

    // PUT /api/cart/items/{itemId} - sua so luong
    @PutMapping("/items/{itemId}")
    public CartResponse updateItem(@PathVariable Long itemId, @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(itemId, request);
    }

    // DELETE /api/cart/items/{itemId} - xoa 1 dong khoi gio
    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(@PathVariable Long itemId) {
        return cartService.removeItem(itemId);
    }
}