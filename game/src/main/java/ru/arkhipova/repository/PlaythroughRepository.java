package ru.arkhipova.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.arkhipova.model.entity.Playthrough;

/**
 * JPA repository for {@link ru.arkhipova.model.entity.Playthrough}.
 */
@Repository
public interface PlaythroughRepository extends JpaRepository<Playthrough, UUID> {
    /**
     * Finds a playthrough by player id and lifecycle status.
     */
    Optional<Playthrough> findByUserIdAndStatus(UUID playerId, Playthrough.PlaythroughStatus status);
}
