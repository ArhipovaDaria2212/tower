package ru.arkhipova.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Dungeon floor metadata generated for a playthrough.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "floors")
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playthrough_id", nullable = false)
    private Playthrough playthrough;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber;

    @Column(nullable = false)
    private Long seed;

    @Column(name = "generator_version", nullable = false)
    private Integer generatorVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Initializes creation timestamp before first insert.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
