package ru.arkhipova.service;

import java.util.List;
import java.util.UUID;
import ru.arkhipova.model.dto.FogChunkDto;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.request.FogRevealRequest;
import ru.arkhipova.model.request.FogUpdateRequest;

public interface FogService {

    void initializeFog(Floor floor);

    List<FogChunkDto> getFogChunks(UUID floorId, UUID playerId);

    List<FogChunkDto> getFogChunksInBounds(UUID floorId, UUID playerId, float minX, float minY, float maxX, float maxY);

    /**
     * Persists fog mask updates and returns the chunks that were saved (for broadcasting).
     */
    List<FogChunkDto> updateFog(UUID floorId, UUID playerId, FogUpdateRequest request);

    /**
     * Reveals fog around the player position and returns the chunks that were touched.
     */
    List<FogChunkDto> revealArea(UUID floorId, UUID playerId, FogRevealRequest request);
}
