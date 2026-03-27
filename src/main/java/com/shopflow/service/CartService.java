package com.shopflow.service;

import com.shopflow.dto.request.CartItemRequest;
import com.shopflow.dto.response.Responses;
import com.shopflow.entity.*;
import com.shopflow.exception.ShopExceptions;
import com.shopflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public Responses.CartView viewCart(String userEmail) {
        Cart cart = resolveCart(userEmail);
        return buildCartView(cart);
    }

    @Transactional
    public Responses.CartView addItem(String userEmail, CartItemRequest req) {
        Cart cart = resolveCart(userEmail);
        Product product = productService.fetchOrThrow(req.getProductId());

        if (!product.isInStock(req.getQuantity())) {
            throw new ShopExceptions.OutOfStockException(product.getName());
        }

        CartItem existingItem = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);
        if (existingItem != null) {
            int updatedQty = existingItem.getQuantity() + req.getQuantity();
            existingItem.setQuantity(updatedQty);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(req.getQuantity())
                    .build();
            cart.getItems().add(newItem);
        }

        return buildCartView(cartRepository.save(cart));
    }

    @Transactional
    public Responses.CartView updateItemQty(String userEmail, Long cartItemId, int newQty) {
        Cart cart = resolveCart(userEmail);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ShopExceptions.ResourceNotFoundException(
                        "Cart item not found: " + cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ShopExceptions.AccessDeniedException("You can only edit your own cart");
        }

        if (!item.getProduct().isInStock(newQty)) {
            throw new ShopExceptions.OutOfStockException(item.getProduct().getName());
        }

        item.setQuantity(newQty);
        cartItemRepository.save(item);
        return buildCartView(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    public Responses.CartView removeItem(String userEmail, Long cartItemId) {
        Cart cart = resolveCart(userEmail);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ShopExceptions.ResourceNotFoundException(
                        "Cart item not found: " + cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ShopExceptions.AccessDeniedException("You can only edit your own cart");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return buildCartView(cartRepository.save(cart));
    }

    Cart resolveCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ShopExceptions.ResourceNotFoundException("User not found"));

        return cartRepository.findByOwner(user)
                .orElseThrow(() -> new ShopExceptions.ResourceNotFoundException("Cart not found"));
    }

    private Responses.CartView buildCartView(Cart cart) {
        List<Responses.CartItemView> itemViews = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            BigDecimal lineTotal = ci.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(ci.getQuantity()));
            Responses.CartItemView itemView = Responses.CartItemView.builder()
                    .cartItemId(ci.getId())
                    .productId(ci.getProduct().getId())
                    .productName(ci.getProduct().getName())
                    .unitPrice(ci.getProduct().getPrice())
                    .quantity(ci.getQuantity())
                    .lineTotal(lineTotal)
                    .build();
            itemViews.add(itemView);
        }

        return Responses.CartView.builder()
                .cartId(cart.getId())
                .items(itemViews)
                .grandTotal(cart.calculateTotal())
                .build();
    }
}
