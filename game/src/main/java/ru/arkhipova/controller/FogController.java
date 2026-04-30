package ru.arkhipova.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.arkhipova.model.dto.FogChunkDto;
import ru.arkhipova.security.UserPrincipal;
import ru.arkhipova.service.FogService;

/**
 * REST endpoints for the initial fog-of-war snapshot.
 *
 * <p>Per-move fog operations (updates and reveals) are intentionally NOT exposed via HTTP — they
 * happen on every player keypress and would otherwise hammer the HTTP filter chain. They are
 * routed through STOMP destinations on {@code /app/floors/{floorId}/fog} (apply-and-broadcast)
 * and {@code /app/floors/{floorId}/fog/reveal} (reveal-and-broadcast); see
 * {@link FogWebSocketController}. Clients subscribe to {@code /topic/floors/{floorId}/fog} to
 * receive incremental chunk updates instead of polling.
 *
 * <p>What stays here is the one-time snapshot the client loads when entering a floor.
 */
@RestController
@RequestMapping("/floors")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Fog of War", description = "Fog of war snapshot endpoint")
public class FogController {

    private final FogService fogService;

    /**
     * Returns all fog chunks for a floor (ownership enforced by the service layer).
     */
    @GetMapping("/{floorId}/fog")
    @Operation(summary = "Get fog snapshot", description = "Get all fog chunks for a floor (called once on floor load)")
    public ResponseEntity<List<FogChunkDto>> getFogChunks(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID floorId) {
        log.debug("Fog chunks requested for floorId={}, playerId={}", floorId, principal.getUserId());
        List<FogChunkDto> chunks = fogService.getFogChunks(floorId, principal.getUserId());
        return ResponseEntity.ok(chunks);
    }
}
