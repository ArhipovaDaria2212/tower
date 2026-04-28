package ru.arkhipova.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import ru.arkhipova.model.request.FogUpdateRequest;
import ru.arkhipova.service.FogService;

/**
 * Handles realtime fog updates from websocket clients.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class FogWebSocketController {

    private final FogService fogService;

    /**
     * Receives fog updates over websocket for a specific floor.
     */
    @MessageMapping("/floors/{floorId}/fog")
    public void updateFog(@DestinationVariable UUID floorId, @Valid FogUpdateRequest request) {
        log.debug("Fog websocket update received for floorId={}", floorId);
        fogService.updateFog(floorId, request);
    }
}
