package ru.arkhipova.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.arkhipova.model.dto.FogChunkDto;
import ru.arkhipova.service.FogService;

@RestController
@RequestMapping("/floors")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Fog of War", description = "Fog of war management")
public class FogController {

    private final FogService fogService;

    /**
     * Returns all fog chunks for a floor.
     */
    @GetMapping("/{floorId}/fog")
    @Operation(summary = "Get fog chunks", description = "Get all fog chunks for a floor")
    public ResponseEntity<List<FogChunkDto>> getFogChunks(@PathVariable UUID floorId) {
        log.debug("Fog chunks requested for floorId={}", floorId);
        List<FogChunkDto> chunks = fogService.getFogChunks(floorId);
        return ResponseEntity.ok(chunks);
    }

    /**
     * Returns fog chunks in requested world bounds.
     */
    @GetMapping("/{floorId}/fog/bounds")
    @Operation(summary = "Get fog in bounds", description = "Get fog chunks within world coordinate bounds")
    public ResponseEntity<List<FogChunkDto>> getFogInBounds(
            @PathVariable UUID floorId,
            @RequestParam Float minX,
            @RequestParam Float minY,
            @RequestParam Float maxX,
            @RequestParam Float maxY) {

        log.debug("Fog bounds requested for floorId={}", floorId);
        List<FogChunkDto> chunks = fogService.getFogChunksInBounds(floorId, minX, minY, maxX, maxY);
        return ResponseEntity.ok(chunks);
    }

    /**
     * Reveals fog around the player for a floor.
     */
    @PostMapping("/{floorId}/fog/reveal")
    @Operation(summary = "Reveal fog area", description = "Reveal fog around player position")
    public ResponseEntity<Void> revealFog(
            @PathVariable UUID floorId,
            @RequestParam Float playerX,
            @RequestParam Float playerY,
            @RequestParam(defaultValue = "5.0") Float radius) {

        log.debug("Fog reveal requested for floorId={}", floorId);
        fogService.revealArea(floorId, playerX, playerY, radius);
        return ResponseEntity.ok().build();
    }
}
