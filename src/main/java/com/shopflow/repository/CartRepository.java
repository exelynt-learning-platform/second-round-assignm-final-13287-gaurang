package com.shopflow.repository;

import com.shopflow.entity.Cart;
import com.shopflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByOwner(User owner);
}
