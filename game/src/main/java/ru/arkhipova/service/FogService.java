package ru.arkhipova.service;

import java.util.List;
import java.util.UUID;
import ru.arkhipova.model.dto.FogChunkDto;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.request.FogUpdateRequest;

public interface FogService {
    void initializeFog(Floor floor);

    List<FogChunkDto> getFogChunks(UUID floorId);

    List<FogChunkDto> getFogChunksInBounds(UUID floorId, float minX, float minY, float maxX, float maxY);

    void updateFog(UUID floorId, FogUpdateRequest request);

    void revealArea(UUID floorId, float playerX, float playerY, float radius);
}
