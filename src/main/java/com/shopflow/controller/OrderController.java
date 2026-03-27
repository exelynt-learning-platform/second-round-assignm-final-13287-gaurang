package com.shopflow.controller;

import com.shopflow.dto.request.OrderRequest;
import com.shopflow.dto.response.Responses;
import com.shopflow.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Responses.OrderView> placeOrder(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody OrderRequest request) {
        String email = principal.getUsername();
        Responses.OrderView order = orderService.placeOrderFromCart(email, request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Responses.OrderView>> myOrders(
            @AuthenticationPrincipal UserDetails principal) {
        String email = principal.getUsername();
        List<Responses.OrderView> orders = orderService.listUserOrders(email);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Responses.OrderView> getOrder(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long orderId) {
        String email = principal.getUsername();
        Responses.OrderView order = orderService.getOrderDetails(email, orderId);
        return new ResponseEntity<>(order, HttpStatus.OK);
    }
}
