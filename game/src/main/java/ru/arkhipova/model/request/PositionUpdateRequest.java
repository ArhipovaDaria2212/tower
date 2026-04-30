package ru.arkhipova.model.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionUpdateRequest {
    @NotNull(message = "Player X is required")
    private Float playerX;

    @NotNull(message = "Player Y is required")
    private Float playerY;

    @NotNull(message = "Floor ID is required")
    private UUID floorId;
}
