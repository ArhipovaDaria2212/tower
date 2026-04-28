package ru.arkhipova.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.arkhipova.model.entity.User;
import ru.arkhipova.model.request.UserLoginRequest;
import ru.arkhipova.model.request.UserRegisterRequest;
import ru.arkhipova.model.response.AuthResponse;
import ru.arkhipova.repository.UserRepository;
import ru.arkhipova.security.JwtTokenProvider;
import ru.arkhipova.service.AuthService;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    /**
     * Registers a new player account and returns a JWT access token.
     */
    @Override
    @Transactional
    public AuthResponse register(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected: email already used={}", request.getEmail());
            throw new IllegalArgumentException("Email already in use");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration rejected: username already used={}", request.getUsername());
            throw new IllegalArgumentException("Username already in use");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: userId={}", user.getId());

        String token = tokenProvider.generateToken(user.getId(), "USER");

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    /**
     * Authenticates an existing user and returns a JWT access token.
     */
    @Override
    public AuthResponse login(UserLoginRequest request) {
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login rejected: invalid password for email={}", request.getEmail());
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = tokenProvider.generateToken(user.getId(), "USER");
        log.info("User logged in: userId={}", user.getId());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
