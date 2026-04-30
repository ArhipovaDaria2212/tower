package ru.arkhipova.service;

import ru.arkhipova.model.request.UserLoginRequest;
import ru.arkhipova.model.request.UserRegisterRequest;
import ru.arkhipova.model.response.AuthResponse;

/**
 * Authentication service.
 */
public interface AuthService {
    /**
     * Registers a new player account and returns a JWT access token.
     */
    AuthResponse register(UserRegisterRequest request);

    /**
     * Authenticates an existing user and returns a JWT access token.
     */
    AuthResponse login(UserLoginRequest request);
}
