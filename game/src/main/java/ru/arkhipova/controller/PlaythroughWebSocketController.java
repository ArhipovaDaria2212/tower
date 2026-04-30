package ru.arkhipova.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import ru.arkhipova.model.request.PositionUpdateRequest;
import ru.arkhipova.security.UserPrincipal;
import ru.arkhipova.service.PlaythroughService;

/**
 * Handles realtime player-state updates over websocket.
 *
 * <p>Position updates are emitted on every keypress on the client; routing them through STOMP
 * avoids per-move HTTP overhead and keeps the connection long-lived.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class PlaythroughWebSocketController {

    private final PlaythroughService playthroughService;

    /**
     * Receives a player position update over websocket.
     */
    @MessageMapping("/playthrough/position")
    public void updatePosition(Principal principal, @Valid PositionUpdateRequest request) {
        UUID playerId = resolvePlayerId(principal);
        log.debug("Position websocket update received for playerId={}", playerId);
        playthroughService.updatePosition(playerId, request);
    }

    private UUID resolvePlayerId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof UserPrincipal user) {
            return user.getUserId();
        }
        throw new IllegalStateException("Unauthenticated STOMP session");
    }
}
