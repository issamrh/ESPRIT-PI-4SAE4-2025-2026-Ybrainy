package com.ybrainy.backend.service;

import com.ybrainy.backend.dto.cart.AddToCartDTO;
import com.ybrainy.backend.dto.cart.CartResponseDTO;
import com.ybrainy.backend.entity.*;
import com.ybrainy.backend.entity.enums.CartAction;
import com.ybrainy.backend.entity.enums.CartStatus;
import com.ybrainy.backend.exception.ResourceNotFoundException;
import com.ybrainy.backend.mapper.CartMapper;
import com.ybrainy.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartHistoryRepository cartHistoryRepository;
    private final PackRepository packRepository;
    private final CartMapper cartMapper;

    public CartResponseDTO getActiveCart(Long userId) {
        log.info("Fetching active cart for user: {}", userId);
        Cart cart = getOrCreateActiveCart(userId);
        return cartMapper.toCartResponseDTO(cart);
    }

    public CartResponseDTO addItemToCart(Long userId, AddToCartDTO dto) {
        log.info("Adding pack {} to cart for user {}", dto.getPackId(), userId);
        Cart cart = getOrCreateActiveCart(userId);
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        Pack pack = packRepository.findById(dto.getPackId())
                .orElseThrow(() -> {
                    log.error("Pack not found: {}", dto.getPackId());
                    return new ResourceNotFoundException("Pack not found");
                });

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getPack().getId().equals(dto.getPackId()))
                .findFirst();

        int quantity = dto.getQuantity();
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + dto.getQuantity());
            item.setSubtotal(item.getQuantity() * item.getPriceAtPurchase());
            log.info("Updated quantity for pack {} in cart", dto.getPackId());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .pack(pack)
                    .priceAtPurchase(pack.getSalePrice())
                    .quantity(dto.getQuantity())
                    .subtotal(pack.getSalePrice() * dto.getQuantity())
                    .build();
            cart.getItems().add(newItem);
            log.info("Added new pack {} to cart", dto.getPackId());
        }

        updateCartTotal(cart);
        Cart saved = cartRepository.save(cart);

        // Log to history
        logAction(userId, CartAction.ADD_ITEM, pack.getTitle(), quantity, saved.getTotalAmount(), saved.getStatus());

        return cartMapper.toCartResponseDTO(saved);
    }

    public CartResponseDTO removeItemFromCart(Long userId, Long itemId) {
        log.info("Removing item {} from cart for user {}", itemId, userId);
        Cart cart = getOrCreateActiveCart(userId);

        Optional<CartItem> itemToRemove = cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst();

        if (itemToRemove.isPresent()) {
            CartItem item = itemToRemove.get();
            String title = item.getPack().getTitle();
            int qty = item.getQuantity();
            cart.getItems().remove(item);
            updateCartTotal(cart);
            Cart saved = cartRepository.save(cart);
            logAction(userId, CartAction.REMOVE_ITEM, title, qty, saved.getTotalAmount(), saved.getStatus());
            return cartMapper.toCartResponseDTO(saved);
        }

        return cartMapper.toCartResponseDTO(cart);
    }

    public CartResponseDTO clearCart(Long userId) {
        log.info("Clearing cart for user {}", userId);
        Cart cart = getOrCreateActiveCart(userId);
        cart.getItems().clear();
        cart.setTotalAmount(0.0);
        Cart saved = cartRepository.save(cart);
        logAction(userId, CartAction.CLEAR_CART, null, null, 0.0, saved.getStatus());
        return cartMapper.toCartResponseDTO(saved);
    }

    public CartResponseDTO checkout(Long userId) {
        log.info("Processing checkout for user {}", userId);
        Cart cart = getOrCreateActiveCart(userId);
        if (cart.getItems().isEmpty()) {
            log.warn("Attempted checkout with empty cart for user {}", userId);
            throw new IllegalStateException("Cannot checkout an empty cart");
        }

        cart.setStatus(CartStatus.CHECKED_OUT);
        Cart saved = cartRepository.save(cart);

        logAction(userId, CartAction.CHECKOUT, null, null, saved.getTotalAmount(), saved.getStatus());

        log.info("Checkout successful for user: {}", userId);
        return cartMapper.toCartResponseDTO(saved);
    }

    private void logAction(Long userId, CartAction action, String packTitle, Integer quantity, Double total,
            CartStatus status) {
        CartHistory history = CartHistory.builder()
                .userId(userId)
                .action(action)
                .packTitle(packTitle)
                .quantity(quantity)
                .totalAmount(total)
                .cartStatus(status)
                .build();
        cartHistoryRepository.save(history);
    }

    private Cart getOrCreateActiveCart(Long userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    log.info("Creating new ACTIVE cart for user: {}", userId);
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
