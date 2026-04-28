package ru.arkhipova.model.dto;

import lombok.*;

/**
 * DTO representing a discovered map icon identifier.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscoveredIconDto {
    /** Unique icon identifier from map generation system. */
    private String iconId;
}
