package ru.arkhipova.service;

import java.util.UUID;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.entity.Playthrough;
import ru.arkhipova.model.response.AdvanceFloorResponse;
import ru.arkhipova.model.response.FloorResponse;

public interface FloorService {
    /**
     * Creates a floor for a playthrough and initializes fog data.
     */
    Floor createFloor(Playthrough playthrough, Integer floorNumber);

    /**
     * Returns the current floor for the active playthrough.
     */
    FloorResponse getCurrentFloor(UUID playerId);

    /**
     * Advances active playthrough to the next floor with paywall check.
     */
    AdvanceFloorResponse advanceFloor(UUID playerId);
}
