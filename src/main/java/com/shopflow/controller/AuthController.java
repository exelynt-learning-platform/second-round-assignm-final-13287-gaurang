package com.shopflow.controller;

import com.shopflow.dto.request.AuthRequest;
import com.shopflow.dto.response.Responses;
import com.shopflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Responses.Auth> register(@Valid @RequestBody AuthRequest.Register request) {
        Responses.Auth result = authService.register(request);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<Responses.Auth> login(@Valid @RequestBody AuthRequest.Login request) {
        Responses.Auth result = authService.login(request);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
