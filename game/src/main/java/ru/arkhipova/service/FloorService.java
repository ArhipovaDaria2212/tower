package ru.arkhipova.service;

import java.util.UUID;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.entity.Playthrough;
import ru.arkhipova.model.response.AdvanceFloorResponse;
import ru.arkhipova.model.response.FloorResponse;

public interface FloorService {
    Floor createFloor(Playthrough playthrough, Integer floorNumber);

    FloorResponse getCurrentFloor(UUID playerId);

    AdvanceFloorResponse advanceFloor(UUID playerId);
}
