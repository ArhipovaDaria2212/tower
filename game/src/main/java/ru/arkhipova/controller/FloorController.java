package ru.arkhipova.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.arkhipova.model.response.AdvanceFloorResponse;
import ru.arkhipova.model.response.FloorResponse;
import ru.arkhipova.security.UserPrincipal;
import ru.arkhipova.service.FloorService;

@RestController
@RequestMapping("/playthrough/active")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Floor", description = "Floor management and progression")
public class FloorController {

    private final FloorService floorService;

    /**
     * Returns the current floor for authenticated player.
     */
    @GetMapping("/floor")
    @Operation(summary = "Get current floor", description = "Get current floor details")
    public ResponseEntity<FloorResponse> getCurrentFloor(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Current floor requested for playerId={}", principal.getUserId());
        FloorResponse response = floorService.getCurrentFloor(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * Advances authenticated player to the next floor.
     */
    @PostMapping("/floor/advance")
    @Operation(summary = "Advance to next floor", description = "Move to the next floor in the dungeon")
    public ResponseEntity<AdvanceFloorResponse> advanceFloor(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Advance floor requested for playerId={}", principal.getUserId());
        AdvanceFloorResponse response = floorService.advanceFloor(principal.getUserId());
        return ResponseEntity.ok(response);
    }
}
