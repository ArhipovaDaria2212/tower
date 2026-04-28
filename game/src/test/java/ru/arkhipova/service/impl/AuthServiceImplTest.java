package ru.arkhipova.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.arkhipova.model.entity.User;
import ru.arkhipova.model.request.UserLoginRequest;
import ru.arkhipova.model.request.UserRegisterRequest;
import ru.arkhipova.repository.UserRepository;
import ru.arkhipova.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerCreatesUserAndReturnsToken() {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("u@test.com")
                .username("user")
                .password("secret123")
                .build();
        UUID userId = UUID.randomUUID();
        User saved = User.builder()
                .id(userId)
                .email(request.getEmail())
                .username(request.getUsername())
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenProvider.generateToken(userId, "USER")).thenReturn("jwt-token");

        var result = authService.register(request);

        assertEquals("jwt-token", result.getToken());
        assertEquals(userId, result.getUserId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginThrowsForInvalidPassword() {
        UserLoginRequest request =
                UserLoginRequest.builder().email("u@test.com").password("bad").build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("u@test.com")
                .password("encoded")
                .build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        verify(jwtTokenProvider, never()).generateToken(any(), any());
    }
}
