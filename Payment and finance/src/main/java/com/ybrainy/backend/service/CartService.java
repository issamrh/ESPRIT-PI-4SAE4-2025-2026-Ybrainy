package com.ybrainy.backend.service;

import com.ybrainy.backend.dto.cart.AddToCartDTO;
import com.ybrainy.backend.dto.cart.CartResponseDTO;
import com.ybrainy.backend.entity.*;
import com.ybrainy.backend.entity.enums.CartStatus;
import com.ybrainy.backend.exception.ResourceNotFoundException;
import com.ybrainy.backend.mapper.CartMapper;
import com.ybrainy.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartHistoryRepository cartHistoryRepository;
    private final PackRepository packRepository;
    private final CartMapper cartMapper;

    public CartResponseDTO getActiveCart(Long userId) {
        Cart cart = getOrCreateActiveCart(userId);
        return cartMapper.toCartResponseDTO(cart);
    }

    public CartResponseDTO addItemToCart(Long userId, AddToCartDTO dto) {
        Cart cart = getOrCreateActiveCart(userId);
        Pack pack = packRepository.findById(dto.getPackId())
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found"));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getPackId().equals(dto.getPackId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + dto.getQuantity());
            item.setSubtotal(item.getQuantity() * item.getPriceAtPurchase());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .packId(pack.getId())
                    .priceAtPurchase(pack.getSalePrice())
                    .quantity(dto.getQuantity())
                    .subtotal(pack.getSalePrice() * dto.getQuantity())
                    .build();
            cart.getItems().add(newItem);
        }

        updateCartTotal(cart);
        Cart saved = cartRepository.save(cart);
        return cartMapper.toCartResponseDTO(saved);
    }

    public CartResponseDTO removeItemFromCart(Long userId, Long itemId) {
        Cart cart = getOrCreateActiveCart(userId);
        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        updateCartTotal(cart);
        Cart saved = cartRepository.save(cart);
        return cartMapper.toCartResponseDTO(saved);
    }

    public CartResponseDTO clearCart(Long userId) {
        Cart cart = getOrCreateActiveCart(userId);
        cart.getItems().clear();
        cart.setTotalAmount(0.0);
        Cart saved = cartRepository.save(cart);
        return cartMapper.toCartResponseDTO(saved);
    }

    public CartResponseDTO checkout(Long userId) {
        Cart cart = getOrCreateActiveCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }

        cart.setStatus(CartStatus.CHECKED_OUT);

        CartHistory history = CartHistory.builder()
                .userId(userId)
                .cartId(cart.getId())
                .totalAmount(cart.getTotalAmount())
                .status(CartStatus.CHECKED_OUT)
                .build();
        cartHistoryRepository.save(history);

        // In a real app, you'd create a new cart after checkout
        // For this implementation, we'll just return the checked out cart
        return cartMapper.toCartResponseDTO(cartRepository.save(cart));
    }

    private Cart getOrCreateActiveCart(Long userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .status(CartStatus.ACTIVE)
                            .totalAmount(0.0)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private void updateCartTotal(Cart cart) {
        double total = cart.getItems().stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();
        cart.setTotalAmount(total);
    }
}
