package ru.arkhipova.service.impl;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.arkhipova.model.dto.FogChunkDto;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.entity.FogChunk;
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

    private static final int CHUNK_SIZE = 16;
    private static final int CELLS_PER_CHUNK = 16;
    private static final int MASK_SIZE = 32;

    /**
     * Creates an initial fully hidden fog chunk for a new floor.
     */
    @Override
    @Transactional
    public void initializeFog(Floor floor) {
        int initialChunkX = 0;
        int initialChunkY = 0;

        byte[] initialMask = new byte[MASK_SIZE];
        Arrays.fill(initialMask, (byte) 0xFF);

        FogChunk chunk = FogChunk.builder()
                .floor(floor)
                .chunkX(initialChunkX)
                .chunkY(initialChunkY)
                .mask(initialMask)
                .build();

        fogChunkRepository.save(chunk);
        log.debug("Initial fog chunk created for floorId={}", floor.getId());
    }

    /**
     * Returns all fog chunks for the floor.
     */
    @Override
    public List<FogChunkDto> getFogChunks(UUID floorId) {
        List<FogChunk> chunks = fogChunkRepository.findByFloorId(floorId);

        return chunks.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Returns fog chunks that intersect requested world bounds.
     */
    @Override
    public List<FogChunkDto> getFogChunksInBounds(UUID floorId, float minX, float minY, float maxX, float maxY) {
        int minChunkX = (int) Math.floor(minX / CHUNK_SIZE);
        int maxChunkX = (int) Math.floor(maxX / CHUNK_SIZE);
        int minChunkY = (int) Math.floor(minY / CHUNK_SIZE);
        int maxChunkY = (int) Math.floor(maxY / CHUNK_SIZE);

        List<FogChunkDto> result = new ArrayList<>();

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int y = minChunkY; y <= maxChunkY; y++) {
                Optional<FogChunk> chunk = fogChunkRepository.findByFloorIdAndChunkXAndChunkY(floorId, x, y);
                chunk.ifPresent(c -> result.add(toDto(c)));
            }
        }

        log.debug("Fog bounds query: floorId={}, chunksReturned={}", floorId, result.size());
        return result;
    }

    /**
     * Saves or updates fog chunk masks for a floor.
     */
    @Override
    @Transactional
    public void updateFog(UUID floorId, FogUpdateRequest request) {
        Floor floor =
                floorRepository.findById(floorId).orElseThrow(() -> new IllegalArgumentException("Floor not found"));

        for (FogChunkDto chunkDto : request.getChunks()) {
            byte[] mask = Base64.getDecoder().decode(chunkDto.getMaskBase64());

            Optional<FogChunk> existing = fogChunkRepository.findByFloorIdAndChunkXAndChunkY(
                    floorId, chunkDto.getChunkX(), chunkDto.getChunkY());

            if (existing.isPresent()) {
                FogChunk chunk = existing.get();
                chunk.setMask(mask);
                fogChunkRepository.save(chunk);
            } else {
                FogChunk chunk = FogChunk.builder()
                        .floor(floor)
                        .chunkX(chunkDto.getChunkX())
                        .chunkY(chunkDto.getChunkY())
                        .mask(mask)
                        .build();
                fogChunkRepository.save(chunk);
            }
        }
        log.debug(
                "Fog updated for floorId={}, chunkCount={}",
                floorId,
                request.getChunks().size());
    }

    /**
     * Reveals rectangular area around player position in fog grid.
     */
    @Override
    @Transactional
    public void revealArea(UUID floorId, float playerX, float playerY, float radius) {
        float minX = playerX - radius;
        float maxX = playerX + radius;
        float minY = playerY - radius;
        float maxY = playerY + radius;

        int minChunkX = (int) Math.floor(minX / CHUNK_SIZE);
        int maxChunkX = (int) Math.floor(maxX / CHUNK_SIZE);
        int minChunkY = (int) Math.floor(minY / CHUNK_SIZE);
        int maxChunkY = (int) Math.floor(maxY / CHUNK_SIZE);

        Floor floor =
                floorRepository.findById(floorId).orElseThrow(() -> new IllegalArgumentException("Floor not found"));

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            final int finalCx = cx;
            for (int cy = minChunkY; cy <= maxChunkY; cy++) {
                final int finalCy = cy;
                FogChunk chunk = fogChunkRepository
                        .findByFloorIdAndChunkXAndChunkY(floorId, finalCx, finalCy)
                        .orElseGet(() -> {
                            FogChunk newChunk = FogChunk.builder()
                                    .floor(floor)
                                    .chunkX(finalCx)
                                    .chunkY(finalCy)
                                    .mask(new byte[MASK_SIZE])
                                    .build();
                            return fogChunkRepository.save(newChunk);
                        });

                revealCellsInChunk(chunk, minX, minY, maxX, maxY, finalCx, finalCy);
                fogChunkRepository.save(chunk);
            }
        }
        log.debug("Fog reveal applied for floorId={}, center=({}, {}), radius={}", floorId, playerX, playerY, radius);
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

        for (int localX = 0; localX < CELLS_PER_CHUNK; localX++) {
            for (int localY = 0; localY < CELLS_PER_CHUNK; localY++) {
                float cellWorldX = chunkWorldMinX + localX;
                float cellWorldY = chunkWorldMinY + localY;

                if (cellWorldX >= worldMinX
                        && cellWorldX <= worldMaxX
                        && cellWorldY >= worldMinY
                        && cellWorldY <= worldMaxY) {

                    int bitIndex = localY * CELLS_PER_CHUNK + localX;
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
