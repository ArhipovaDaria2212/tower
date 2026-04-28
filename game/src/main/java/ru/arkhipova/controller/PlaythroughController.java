package ru.arkhipova.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.arkhipova.model.request.PositionUpdateRequest;
import ru.arkhipova.model.response.PlaythroughResponse;
import ru.arkhipova.security.UserPrincipal;
import ru.arkhipova.service.PlaythroughService;

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
        return ResponseEntity.ok(response);
    }

    /**
     * Updates player position on the current floor.
     */
    @PutMapping("/position")
    @Operation(summary = "Update player position", description = "Update player coordinates in current floor")
    public ResponseEntity<PlaythroughResponse> updatePosition(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody PositionUpdateRequest request) {
        log.debug("Position update requested for playerId={}", principal.getUserId());
        PlaythroughResponse response = playthroughService.updatePosition(principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
