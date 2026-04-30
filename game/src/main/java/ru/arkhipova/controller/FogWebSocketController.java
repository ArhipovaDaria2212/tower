package ru.arkhipova.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import ru.arkhipova.model.dto.FogChunkDto;
import ru.arkhipova.model.request.FogRevealRequest;
import ru.arkhipova.model.request.FogUpdateRequest;
import ru.arkhipova.security.UserPrincipal;
import ru.arkhipova.service.FogService;

/**
 * STOMP endpoints for high-frequency fog operations.
 *
 * <p>Both handlers persist the change and then broadcast the resulting chunks to every subscriber
 * of {@code /topic/floors/{floorId}/fog}. This collapses the previous "POST reveal → GET bounds"
 * polling loop into a single round-trip plus a server-pushed update, and keeps the per-move
 * overhead off the HTTP filter chain.
 *
 * <p>Floor ownership is enforced inside {@link FogService}; this controller only resolves the
 * authenticated principal from the STOMP session.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class FogWebSocketController {

    private static final String FOG_TOPIC_PREFIX = "/topic/floors/";
    private static final String FOG_TOPIC_SUFFIX = "/fog";

    private final FogService fogService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Persists fog mask updates pushed by the client and broadcasts the saved chunks.
     */
    @MessageMapping("/floors/{floorId}/fog")
    public void updateFog(Principal principal, @DestinationVariable UUID floorId, @Valid FogUpdateRequest request) {
        UUID playerId = resolvePlayerId(principal);
        List<FogChunkDto> saved = fogService.updateFog(floorId, playerId, request);
        broadcast(floorId, saved);
    }

    /**
     * Reveals fog around the player's current position and broadcasts the affected chunks.
     */
    @MessageMapping("/floors/{floorId}/fog/reveal")
    public void revealFog(Principal principal, @DestinationVariable UUID floorId, @Valid FogRevealRequest request) {
        UUID playerId = resolvePlayerId(principal);
        List<FogChunkDto> saved = fogService.revealArea(floorId, playerId, request);
        broadcast(floorId, saved);
    }

    private void broadcast(UUID floorId, List<FogChunkDto> chunks) {
        if (chunks.isEmpty()) {
            return;
        }
        messagingTemplate.convertAndSend(FOG_TOPIC_PREFIX + floorId + FOG_TOPIC_SUFFIX, chunks);
    }

    private UUID resolvePlayerId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof UserPrincipal user) {
            return user.getUserId();
        }
        throw new IllegalStateException("Unauthenticated STOMP session");
    }
}
