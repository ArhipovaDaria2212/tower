package ru.arkhipova.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.arkhipova.model.entity.Floor;

/**
 * JPA repository for {@link ru.arkhipova.model.entity.Floor}.
 */
@Repository
public interface FloorRepository extends JpaRepository<Floor, UUID> {
    /**
     * Returns all floors of a playthrough ordered from latest to earliest.
     */
    List<Floor> findByPlaythroughIdOrderByFloorNumberDesc(UUID playthroughId);

    /**
     * Finds a specific floor in a playthrough by floor number.
     */
    Optional<Floor> findByPlaythroughIdAndFloorNumber(UUID playthroughId, Integer floorNumber);
}
