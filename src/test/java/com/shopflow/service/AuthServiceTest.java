package com.shopflow.service;

import com.shopflow.dto.request.AuthRequest;
import com.shopflow.dto.response.Responses;
import com.shopflow.entity.Cart;
import com.shopflow.entity.User;
import com.shopflow.exception.ShopExceptions;
import com.shopflow.repository.CartRepository;
import com.shopflow.repository.UserRepository;
import com.shopflow.util.JwtHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock CartRepository cartRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authManager;
    @Mock JwtHelper jwtHelper;

    @InjectMocks
    AuthService authService;

    private AuthRequest.Register registerReq;
    private AuthRequest.Login loginReq;

    @BeforeEach
    void setUp() {
        registerReq = new AuthRequest.Register();
        registerReq.setFullName("Gaurang Mali");
        registerReq.setEmail("gaurang@shopflow.com");
        registerReq.setPassword("secure123");

        loginReq = new AuthRequest.Login();
        loginReq.setEmail("gaurang@shopflow.com");
        loginReq.setPassword("secure123");
    }

    @Test
    @DisplayName("Registering a new user should return token and save cart")
    void register_newUser_returnsAuthResponseAndPersistsCart() {
        when(userRepository.existsByEmail(registerReq.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerReq.getPassword())).thenReturn("hashed_pw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtHelper.generateToken(registerReq.getEmail())).thenReturn("mock.jwt.token");

        Responses.Auth result = authService.register(registerReq);

        assertThat(result.getToken()).isEqualTo("mock.jwt.token");
        assertThat(result.getEmail()).isEqualTo("gaurang@shopflow.com");
        assertThat(result.getRole()).isEqualTo("CUSTOMER");

        verify(cartRepository).save(any(Cart.class));  // cart must be created
    }

    @Test
    @DisplayName("Registering with an already-used email should throw EmailAlreadyRegisteredException")
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail(registerReq.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerReq))
                .isInstanceOf(ShopExceptions.EmailAlreadyRegisteredException.class)
                .hasMessageContaining("gaurang@shopflow.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login with valid credentials returns a JWT")
    void login_validCredentials_returnsToken() {
        User stubUser = User.builder()
                .id(1L).email(loginReq.getEmail())
                .fullName("Gaurang Mali")
                .passwordHash("hashed_pw")
                .role(User.Role.CUSTOMER)
                .build();

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // authentication passes
        when(userRepository.findByEmail(loginReq.getEmail())).thenReturn(Optional.of(stubUser));
        when(jwtHelper.generateToken(loginReq.getEmail())).thenReturn("login.jwt.token");

        Responses.Auth result = authService.login(loginReq);

        assertThat(result.getToken()).isEqualTo("login.jwt.token");
        assertThat(result.getFullName()).isEqualTo("Gaurang Mali");
    }

    @Test
    @DisplayName("Login with wrong password should propagate BadCredentialsException")
    void login_wrongPassword_throwsBadCredentials() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad creds"));

        assertThatThrownBy(() -> authService.login(loginReq))
                .isInstanceOf(BadCredentialsException.class);
    }
}
