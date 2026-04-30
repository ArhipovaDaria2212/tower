package ru.arkhipova.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * Player-position payload that drives a fog reveal over websocket.
 *
 * <p>Sent on every move, so it is intentionally minimal — the floor id comes from the STOMP
 * destination variable, not the body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FogRevealRequest {

    @NotNull(message = "Player X is required")
    private Float playerX;

    @NotNull(message = "Player Y is required")
    private Float playerY;

    @Positive(message = "Radius must be positive")
    private Float radius = 5.0f;
}
