package ru.arkhipova.service.impl;

import static ru.arkhipova.service.FogConstants.CELLS_PER_CHUNK_AXIS;
import static ru.arkhipova.service.FogConstants.CHUNK_SIZE;
import static ru.arkhipova.service.FogConstants.MASK_SIZE_BYTES;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.arkhipova.exception.ForbiddenException;
import ru.arkhipova.model.dto.FogChunkDto;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.entity.FogChunk;
import ru.arkhipova.model.request.FogRevealRequest;
import ru.arkhipova.model.request.FogUpdateRequest;
import ru.arkhipova.repository.FloorRepository;
import ru.arkhipova.repository.FogChunkRepository;
import ru.arkhipova.service.FogService;

@Service
@Slf4j
@RequiredArgsConstructor
public class FogServiceImpl implements FogService {

    private final FogChunkRepository fogChunkRepository;
    private final FloorRepository floorRepository;

    /**
     * Creates an initial fully exposed fog layer for the new floor.
     */
    @Override
    @Transactional
    public void initializeFog(Floor floor) {
        byte[] initialMask = new byte[MASK_SIZE_BYTES];
        Arrays.fill(initialMask, (byte) 0xFF);

        FogChunk chunk = FogChunk.builder().floor(floor).mask(initialMask).build();

        fogChunkRepository.save(chunk);
        log.debug("Initial fog chunk created for floorId={}", floor.getId());
    }

    /**
     * Returns all fog chunks for the floor (read-only).
     */
    @Override
    @Transactional(readOnly = true)
    public List<FogChunkDto> getFogChunks(UUID floorId, UUID playerId) {
        assertFloorOwnership(floorId, playerId);
        List<FogChunk> chunks = fogChunkRepository.findByFloorId(floorId);
        return chunks.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Returns fog chunks that intersect the requested world bounds. One DB query, not N×M.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FogChunkDto> getFogChunksInBounds(
            UUID floorId, UUID playerId, float minX, float minY, float maxX, float maxY) {
        assertFloorOwnership(floorId, playerId);

        int minChunkX = getChunk(minX);
        int maxChunkX = getChunk(maxX);
        int minChunkY = getChunk(minY);
        int maxChunkY = getChunk(maxY);

        List<FogChunk> chunks = fogChunkRepository.findByFloorIdAndChunkXBetweenAndChunkYBetween(
                floorId, minChunkX, maxChunkX, minChunkY, maxChunkY);

        log.debug("Fog bounds query: floorId={}, chunksReturned={}", floorId, chunks.size());
        return chunks.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Saves or updates fog chunk masks for a floor and returns the chunks that were persisted.
     */
    @Override
    @Transactional
    public List<FogChunkDto> updateFog(UUID floorId, UUID playerId, FogUpdateRequest request) {
        Floor floor = assertFloorOwnership(floorId, playerId);

        List<FogChunk> savedChunks = new ArrayList<>(request.getChunks().size());
        for (FogChunkDto chunkDto : request.getChunks()) {
            byte[] mask = Base64.getDecoder().decode(chunkDto.getMaskBase64());
            if (mask.length != MASK_SIZE_BYTES) {
                throw new IllegalArgumentException(
                        "Invalid fog mask length: expected " + MASK_SIZE_BYTES + " bytes, got " + mask.length);
            }

            Optional<FogChunk> existing = fogChunkRepository.findByFloorIdAndChunkXAndChunkY(
                    floorId, chunkDto.getChunkX(), chunkDto.getChunkY());

            FogChunk chunk;
            if (existing.isPresent()) {
                chunk = existing.get();
                chunk.setMask(mask);
            } else {
                chunk = FogChunk.builder()
                        .floor(floor)
                        .chunkX(chunkDto.getChunkX())
                        .chunkY(chunkDto.getChunkY())
                        .mask(mask)
                        .build();
            }
            savedChunks.add(fogChunkRepository.save(chunk));
        }
        log.debug(
                "Fog updated for floorId={}, chunkCount={}",
                floorId,
                request.getChunks().size());
        return savedChunks.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Reveals rectangular area around player position. Bulk-loads all touched chunks once,
     * mutates them in memory, persists with a single saveAll, and returns the resulting state of
     * every touched chunk so callers can broadcast incremental updates.
     */
    @Override
    @Transactional
    public List<FogChunkDto> revealArea(UUID floorId, UUID playerId, FogRevealRequest request) {
        Floor floor = assertFloorOwnership(floorId, playerId);
        Float playerX = request.getPlayerX();
        Float playerY = request.getPlayerY();
        Float radius = request.getRadius();

        float minX = playerX - radius;
        float maxX = playerX + radius;
        float minY = playerY - radius;
        float maxY = playerY + radius;

        int minChunkX = getChunk(minX);
        int maxChunkX = getChunk(maxX);
        int minChunkY = getChunk(minY);
        int maxChunkY = getChunk(maxY);

        List<FogChunk> chunks = fogChunkRepository.findByFloorIdAndChunkXBetweenAndChunkYBetween(
                floorId, minChunkX, maxChunkX, minChunkY, maxChunkY);
        Map<Long, FogChunk> byCoord = new HashMap<>();
        for (FogChunk c : chunks) {
            byCoord.put(getCompositeKeyByCoordinate(c.getChunkX(), c.getChunkY()), c);
        }

        List<FogChunk> toSave = new ArrayList<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cy = minChunkY; cy <= maxChunkY; cy++) {
                FogChunk chunk = byCoord.get(getCompositeKeyByCoordinate(cx, cy));
                if (chunk == null) {
                    chunk = FogChunk.builder()
                            .floor(floor)
                            .chunkX(cx)
                            .chunkY(cy)
                            .mask(new byte[MASK_SIZE_BYTES])
                            .build();
                }
                revealCellsInChunk(chunk, minX, minY, maxX, maxY, cx, cy);
                toSave.add(chunk);
            }
        }
        List<FogChunk> saved = fogChunkRepository.saveAll(toSave);
        log.debug(
                "Fog reveal applied for floorId={}, center=({}, {}), radius={}, chunks={}",
                floorId,
                playerX,
                playerY,
                radius,
                saved.size());
        return saved.stream().map(this::toDto).collect(Collectors.toList());
    }

    private static int getChunk(float coordinate) {
        return (int) Math.floor(coordinate / CHUNK_SIZE);
    }

    private Floor assertFloorOwnership(UUID floorId, UUID playerId) {
        Floor floor =
                floorRepository.findById(floorId).orElseThrow(() -> new IllegalArgumentException("Floor not found"));
        UUID owner = floor.getPlaythrough() != null && floor.getPlaythrough().getUser() != null
                ? floor.getPlaythrough().getUser().getId()
                : null;
        if (owner == null || !owner.equals(playerId)) {
            log.warn("Forbidden fog access: floorId={}, callerId={}, ownerId={}", floorId, playerId, owner);
            throw new ForbiddenException("Floor does not belong to the authenticated player");
        }
        return floor;
    }

    private static long getCompositeKeyByCoordinate(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xFFFFFFFFL);
    }

    private void revealCellsInChunk(
            FogChunk chunk,
            float worldMinX,
            float worldMinY,
            float worldMaxX,
            float worldMaxY,
            int chunkX,
            int chunkY) {
        byte[] mask = chunk.getMask();

        float chunkWorldMinX = chunkX * CHUNK_SIZE;
        float chunkWorldMinY = chunkY * CHUNK_SIZE;

        for (int localX = 0; localX < CELLS_PER_CHUNK_AXIS; localX++) {
            for (int localY = 0; localY < CELLS_PER_CHUNK_AXIS; localY++) {
                float cellWorldX = chunkWorldMinX + localX;
                float cellWorldY = chunkWorldMinY + localY;

                if (cellWorldX >= worldMinX
                        && cellWorldX <= worldMaxX
                        && cellWorldY >= worldMinY
                        && cellWorldY <= worldMaxY) {

                    int bitIndex = localY * CELLS_PER_CHUNK_AXIS + localX;
                    int byteIndex = bitIndex / 8;
                    int bitOffset = bitIndex % 8;

                    mask[byteIndex] = (byte) (mask[byteIndex] | (1 << bitOffset));
                }
            }
        }

        chunk.setMask(mask);
    }

    private FogChunkDto toDto(FogChunk chunk) {
        return FogChunkDto.builder()
                .chunkX(chunk.getChunkX())
                .chunkY(chunk.getChunkY())
                .maskBase64(Base64.getEncoder().encodeToString(chunk.getMask()))
                .build();
    }
}
