package com.shopflow.service;

import com.shopflow.dto.request.AuthRequest;
import com.shopflow.dto.response.Responses;
import com.shopflow.entity.Cart;
import com.shopflow.entity.User;
import com.shopflow.exception.ShopExceptions;
import com.shopflow.repository.CartRepository;
import com.shopflow.repository.UserRepository;
import com.shopflow.util.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtHelper jwtHelper;

    @Transactional
    public Responses.Auth register(AuthRequest.Register req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ShopExceptions.EmailAlreadyRegisteredException(req.getEmail());
        }

        User newUser = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.CUSTOMER)
                .build();

        userRepository.save(newUser);

        // every new user gets an empty cart right away
        Cart freshCart = Cart.builder().owner(newUser).build();
        cartRepository.save(freshCart);

        log.info("New customer registered: {}", newUser.getEmail());

        String token = jwtHelper.generateToken(newUser.getEmail());
        return buildAuthResponse(newUser, token);
    }

    public Responses.Auth login(AuthRequest.Login req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ShopExceptions.ResourceNotFoundException("User not found"));

        String token = jwtHelper.generateToken(user.getEmail());
        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user, token);
    }

    private Responses.Auth buildAuthResponse(User user, String token) {
        return Responses.Auth.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}
