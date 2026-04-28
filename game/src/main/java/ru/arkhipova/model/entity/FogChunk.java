package ru.arkhipova.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Fog-of-war chunk with bit mask of hidden/revealed cells.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fog_chunks")
public class FogChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Column(name = "chunk_x", nullable = false)
    private Integer chunkX;

    @Column(name = "chunk_y", nullable = false)
    private Integer chunkY;

    @Column(name = "mask")
    private byte[] mask;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Initializes update timestamp before first insert.
     */
    @PrePersist
    protected void onCreate() {
        updatedAt = Instant.now();
    }

    /**
     * Refreshes update timestamp before entity update.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
