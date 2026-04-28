package ru.arkhipova.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Represents a single game run for a player.
 */
@Entity
@Table(name = "playthroughs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Playthrough {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_floor_id")
    private Floor currentFloor;

    @Column(name = "player_x")
    private Float playerX;

    @Column(name = "player_y")
    private Float playerY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaythroughStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Initializes audit timestamps before first insert.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    /**
     * Updates audit timestamp before entity update.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Lifecycle state of a playthrough.
     */
    public enum PlaythroughStatus {
        ACTIVE,
        COMPLETED
    }
}
