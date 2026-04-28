package ru.arkhipova.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.arkhipova.model.entity.Entitlement;

/**
 * JPA repository for {@link ru.arkhipova.model.entity.Entitlement}.
 */
@Repository
public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {
    /**
     * Finds entitlement record for a player.
     */
    Optional<Entitlement> findByUserId(UUID playerId);
}
