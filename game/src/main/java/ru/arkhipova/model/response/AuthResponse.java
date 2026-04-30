package ru.arkhipova.model.response;

import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private UUID userId;
    private String username;
    private String email;
}
