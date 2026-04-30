package ru.arkhipova.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.arkhipova.model.response.PlaythroughResponse;
import ru.arkhipova.security.UserPrincipal;
import ru.arkhipova.service.PlaythroughService;

/**
 * REST endpoints for playthrough lifecycle.
 *
 * <p>Player position updates are intentionally NOT exposed here — they are emitted on every keypress
 * and are routed through the {@code /app/playthrough/position} STOMP endpoint instead, see
 * {@link PlaythroughWebSocketController}.
 */
@RestController
@RequestMapping("/playthrough")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Playthrough", description = "Game playthrough management")
public class PlaythroughController {

    private final PlaythroughService playthroughService;

    /**
     * Creates a new active playthrough for the authenticated player.
     */
    @PostMapping
    @Operation(summary = "Create playthrough", description = "Create a new game playthrough")
    public ResponseEntity<PlaythroughResponse> createPlaythrough(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Create playthrough requested for playerId={}", principal.getUserId());
        PlaythroughResponse response = playthroughService.createPlaythrough(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the active playthrough for the authenticated player.
     */
    @GetMapping("/active")
    @Operation(summary = "Get active playthrough", description = "Get current active playthrough")
    public ResponseEntity<PlaythroughResponse> getActivePlaythrough(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Get active playthrough requested for playerId={}", principal.getUserId());
        PlaythroughResponse response = playthroughService.getActivePlaythrough(principal.getUserId());
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
