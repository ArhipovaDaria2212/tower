package ru.arkhipova.model.response;

import java.util.UUID;
import lombok.*;
import ru.arkhipova.model.entity.Playthrough;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaythroughResponse {
    private UUID id;
    private UUID userId;
    private UUID currentFloorId;
    private Integer currentFloorNumber;
    private Float playerX;
    private Float playerY;
    private Playthrough.PlaythroughStatus status;
}
