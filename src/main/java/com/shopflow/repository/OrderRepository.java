package com.shopflow.repository;

import com.shopflow.entity.Order;
import com.shopflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByPlacedByOrderByOrderedAtDesc(User user);
    Optional<Order> findByStripePaymentIntentId(String intentId);
}
