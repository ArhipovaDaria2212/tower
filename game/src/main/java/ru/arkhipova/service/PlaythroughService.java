package ru.arkhipova.service;

import java.util.UUID;
import ru.arkhipova.model.request.PositionUpdateRequest;
import ru.arkhipova.model.response.PlaythroughResponse;

public interface PlaythroughService {
    PlaythroughResponse createPlaythrough(UUID playerId);

    PlaythroughResponse getActivePlaythrough(UUID playerId);

    PlaythroughResponse updatePosition(UUID playerId, PositionUpdateRequest request);
}
