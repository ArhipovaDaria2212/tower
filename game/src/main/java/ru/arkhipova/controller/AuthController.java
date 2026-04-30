package ru.arkhipova.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.arkhipova.model.request.UserLoginRequest;
import ru.arkhipova.model.request.UserRegisterRequest;
import ru.arkhipova.model.response.AuthResponse;
import ru.arkhipova.service.AuthService;

@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user account.
     */
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new user account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        log.debug("User registration requested for email={}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Authenticates a user by email and password.
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and get JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody UserLoginRequest request) {
        log.debug("User login requested for email={}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
