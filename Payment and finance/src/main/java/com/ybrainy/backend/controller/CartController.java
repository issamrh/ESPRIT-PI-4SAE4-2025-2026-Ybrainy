package com.ybrainy.backend.controller;

import com.ybrainy.backend.dto.cart.AddToCartDTO;
import com.ybrainy.backend.dto.cart.CartHistoryResponseDTO;
import com.ybrainy.backend.dto.cart.CartResponseDTO;
import com.ybrainy.backend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*") // Fix for allowCredentials issue
public class CartController {

    private final CartService cartService;

    // TODO: Get real userId from SecurityContext
    private static final Long TEMP_USER_ID = 1L;

    @GetMapping
    public ResponseEntity<CartResponseDTO> getCart() {
        return ResponseEntity.ok(cartService.getActiveCart(TEMP_USER_ID));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDTO> addItem(@Valid @RequestBody AddToCartDTO dto) {
        return ResponseEntity.ok(cartService.addItemToCart(TEMP_USER_ID, dto));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDTO> removeItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItemFromCart(TEMP_USER_ID, itemId));
    }

    @DeleteMapping
    public ResponseEntity<CartResponseDTO> clearCart() {
        return ResponseEntity.ok(cartService.clearCart(TEMP_USER_ID));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CartResponseDTO> checkout() {
        return ResponseEntity.ok(cartService.checkout(TEMP_USER_ID));
    }

    @GetMapping("/history")
    public ResponseEntity<List<CartHistoryResponseDTO>> getHistory() {
        return ResponseEntity.ok(cartService.getCartHistory(TEMP_USER_ID));
    }
}
