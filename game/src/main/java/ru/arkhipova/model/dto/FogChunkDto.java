package ru.arkhipova.model.dto;

import lombok.*;

/**
 * Transport object for fog chunk payload between API and client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FogChunkDto {
    /** Chunk coordinate on X axis. */
    private Integer chunkX;
    /** Chunk coordinate on Y axis. */
    private Integer chunkY;
    /** Fog bit mask encoded as Base64 string. */
    private String maskBase64;
}
