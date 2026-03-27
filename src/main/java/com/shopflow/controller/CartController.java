package com.shopflow.controller;

import com.shopflow.dto.request.CartItemRequest;
import com.shopflow.dto.response.Responses;
import com.shopflow.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<Responses.CartView> viewCart(
            @AuthenticationPrincipal UserDetails principal) {
        String email = principal.getUsername();
        Responses.CartView result = cartService.viewCart(email);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/items")
    public ResponseEntity<Responses.CartView> addToCart(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CartItemRequest request) {
        String email = principal.getUsername();
        Responses.CartView result = cartService.addItem(email, request);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<Responses.CartView> updateQty(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {
        String email = principal.getUsername();
        Responses.CartView result = cartService.updateItemQty(email, cartItemId, quantity);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Responses.CartView> removeFromCart(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long cartItemId) {
        String email = principal.getUsername();
        Responses.CartView result = cartService.removeItem(email, cartItemId);
        return ResponseEntity.ok(result);
    }
}
