package ru.arkhipova.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.arkhipova.model.entity.User;

/**
 * JPA repository for {@link ru.arkhipova.model.entity.User}.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Finds a registered user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether email is already used.
     */
    boolean existsByEmail(String email);

    /**
     * Checks whether username is already used.
     */
    boolean existsByUsername(String username);
}
