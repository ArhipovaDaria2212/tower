package ru.arkhipova.service;

import ru.arkhipova.model.request.UserLoginRequest;
import ru.arkhipova.model.request.UserRegisterRequest;
import ru.arkhipova.model.response.AuthResponse;

public interface AuthService {
    AuthResponse register(UserRegisterRequest request);

    AuthResponse login(UserLoginRequest request);
}
