package ru.arkhipova.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.arkhipova.model.entity.FogChunk;

/**
 * JPA repository for {@link ru.arkhipova.model.entity.FogChunk}.
 */
@Repository
public interface FogChunkRepository extends JpaRepository<FogChunk, UUID> {
    /**
     * Returns all fog chunks belonging to a floor.
     */
    List<FogChunk> findByFloorId(UUID floorId);

    /**
     * Finds a specific fog chunk by floor and chunk coordinates.
     */
    Optional<FogChunk> findByFloorIdAndChunkXAndChunkY(UUID floorId, Integer chunkX, Integer chunkY);

    /**
     * Returns chunks of a floor whose coordinates lie within given inclusive ranges.
     *
     * <p>Used to fetch all chunks intersecting a viewport in a single query, instead of issuing
     * one lookup per (chunkX, chunkY) pair.
     */
    List<FogChunk> findByFloorIdAndChunkXBetweenAndChunkYBetween(
            UUID floorId, Integer minChunkX, Integer maxChunkX, Integer minChunkY, Integer maxChunkY);
}
