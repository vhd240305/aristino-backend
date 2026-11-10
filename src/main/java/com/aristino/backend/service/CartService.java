package com.aristino.backend.service;

import com.aristino.backend.common.CurrentUserProvider;
import com.aristino.backend.dto.cart.AddCartItemRequest;
import com.aristino.backend.dto.cart.CartResponse;
import com.aristino.backend.dto.cart.UpdateCartItemRequest;
import com.aristino.backend.entity.Cart;
import com.aristino.backend.entity.CartItem;
import com.aristino.backend.entity.ProductVariant;
import com.aristino.backend.entity.User;
import com.aristino.backend.repository.CartItemRepository;
import com.aristino.backend.repository.CartRepository;
import com.aristino.backend.repository.ProductVariantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final CurrentUserProvider currentUserProvider;

    public CartResponse getMyCart() {
        return CartResponse.from(getOrCreateCart());
    }

    public CartResponse addItem(AddCartItemRequest request) {
        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay bien the san pham"));

        Cart cart = getOrCreateCart();

        // Neu bien the nay da co san trong gio (dung UNIQUE(cart_id, variant_id) da thiet ke)
        // thi cong don so luong, khong tao dong moi
        CartItem item = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId())
                .orElse(null);

        if (item != null) {
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            item = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .build();
        }
        cartItemRepository.save(item);

        return CartResponse.from(cartRepository.findById(cart.getId()).orElseThrow());
    }

    public CartResponse updateItem(Long itemId, UpdateCartItemRequest request) {
        CartItem item = getOwnedCartItem(itemId);
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return CartResponse.from(cartRepository.findById(item.getCart().getId()).orElseThrow());
    }

    public CartResponse removeItem(Long itemId) {
        CartItem item = getOwnedCartItem(itemId);
        Cart cart = item.getCart();
        cart.getItems().remove(item); // orphanRemoval = true: Hibernate tự xóa dòng trong DB khi commit
        return CartResponse.from(cart);
    }

    // Dam bao cart_item thao tac dung la cua chinh user dang dang nhap,
    // tranh truong hop user A sua/xoa duoc gio hang cua user B qua ID
    private CartItem getOwnedCartItem(Long itemId) {
        User user = currentUserProvider.getCurrentUser();
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay san pham trong gio"));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Ban khong co quyen voi gio hang nay");
        }
        return item;
    }

    // Ve nguyen tac moi user da co san 1 gio hang rong tao luc dang ky (xem AuthService),
    // ham nay chi la phong ngua truong hop du lieu cu chua co gio hang
    private Cart getOrCreateCart() {
        User user = currentUserProvider.getCurrentUser();
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }
}