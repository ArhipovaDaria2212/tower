package ru.arkhipova.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.arkhipova.model.entity.Floor;

/**
 * JPA repository for {@link ru.arkhipova.model.entity.Floor}.
 */
@Repository
public interface FloorRepository extends JpaRepository<Floor, UUID> {}
