package com.ybrainy.backend.controller;

import com.ybrainy.backend.dto.cart.AddToCartDTO;
import com.ybrainy.backend.dto.cart.CartHistoryResponseDTO;
import com.ybrainy.backend.dto.cart.CartResponseDTO;
import com.ybrainy.backend.dto.cart.StripeCheckoutConfirmRequestDTO;
import com.ybrainy.backend.dto.cart.StripeCheckoutSessionResponseDTO;
import com.ybrainy.backend.exception.BusinessRuleException;
import com.ybrainy.backend.service.CartService;
import com.ybrainy.backend.service.CheckoutEmailService;
import com.ybrainy.backend.service.StaticAuthService;
import com.ybrainy.backend.service.StripeCheckoutService;
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
    private final StripeCheckoutService stripeCheckoutService;
    private final CheckoutEmailService checkoutEmailService;
    private final StaticAuthService staticAuthService;

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

    @PostMapping("/checkout/stripe-session")
    public ResponseEntity<StripeCheckoutSessionResponseDTO> createStripeCheckoutSession() {
        return ResponseEntity.ok(cartService.createStripeCheckoutSession(TEMP_USER_ID));
    }

    @PostMapping("/checkout/stripe-confirm")
    public ResponseEntity<CartResponseDTO> confirmStripeCheckout(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody StripeCheckoutConfirmRequestDTO request) {

        boolean paid = stripeCheckoutService.isCheckoutSessionPaid(request.getSessionId());
        if (!paid) {
            throw new BusinessRuleException("Stripe payment is not completed.");
        }

        CartResponseDTO activeCart = cartService.getActiveCart(TEMP_USER_ID);
        if (activeCart.getItems() == null || activeCart.getItems().isEmpty()) {
            return ResponseEntity.ok(activeCart);
        }

        CartResponseDTO checkedOutCart = cartService.checkout(TEMP_USER_ID);
        String recipientEmail = staticAuthService.resolveEmailFromBearerOrDefault(authorization);
        checkoutEmailService.sendCheckoutReceipt(recipientEmail, checkedOutCart);
        return ResponseEntity.ok(checkedOutCart);
    }

    @GetMapping("/history")
    public ResponseEntity<List<CartHistoryResponseDTO>> getHistory() {
        return ResponseEntity.ok(cartService.getCartHistory(TEMP_USER_ID));
    }
}
