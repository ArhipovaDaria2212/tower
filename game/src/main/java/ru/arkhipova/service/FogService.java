package ru.arkhipova.service;

import java.util.List;
import java.util.UUID;
import ru.arkhipova.model.dto.FogChunkDto;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.request.FogRevealRequest;
import ru.arkhipova.model.request.FogUpdateRequest;

public interface FogService {

    /**
     * Creates an initial fully exposed fog layer for the new floor.
     */
    void initializeFog(Floor floor);

    /**
     * Returns all fog chunks for the floor (read-only).
     */
    List<FogChunkDto> getFogChunks(UUID floorId, UUID playerId);

    /**
     * Persists fog mask updates and returns the chunks that were saved (for broadcasting).
     */
    List<FogChunkDto> updateFog(UUID floorId, UUID playerId, FogUpdateRequest request);

    /**
     * Reveals fog around the player position and returns the chunks that were touched.
     */
    List<FogChunkDto> revealArea(UUID floorId, UUID playerId, FogRevealRequest request);
}
