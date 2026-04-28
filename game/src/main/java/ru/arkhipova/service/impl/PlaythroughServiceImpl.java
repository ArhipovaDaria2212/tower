package ru.arkhipova.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.arkhipova.model.entity.Entitlement;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.entity.Playthrough;
import ru.arkhipova.model.request.PositionUpdateRequest;
import ru.arkhipova.model.response.PlaythroughResponse;
import ru.arkhipova.repository.EntitlementRepository;
import ru.arkhipova.repository.PlaythroughRepository;
import ru.arkhipova.repository.UserRepository;
import ru.arkhipova.service.FloorService;
import ru.arkhipova.service.PlaythroughService;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlaythroughServiceImpl implements PlaythroughService {

    private final PlaythroughRepository playthroughRepository;
    private final UserRepository userRepository;
    private final EntitlementRepository entitlementRepository;
    private final FloorService floorService;

    /**
     * Creates a new active playthrough for the player or returns existing one.
     */
    @Override
    @Transactional
    public PlaythroughResponse createPlaythrough(UUID playerId) {
        var user = userRepository.findById(playerId).orElseThrow();
        var existing = playthroughRepository.findByUserIdAndStatus(playerId, Playthrough.PlaythroughStatus.ACTIVE);

        if (existing.isPresent()) {
            log.info(
                    "Active playthrough reused for playerId={}, playthroughId={}",
                    playerId,
                    existing.get().getId());
            return toResponse(existing.get());
        }

        Playthrough playthrough = Playthrough.builder()
                .user(user)
                .status(Playthrough.PlaythroughStatus.ACTIVE)
                .playerX(0f)
                .playerY(0f)
                .build();

        playthrough = playthroughRepository.save(playthrough);

        Floor firstFloor = floorService.createFloor(playthrough, 1);
        playthrough.setCurrentFloor(firstFloor);
        playthrough = playthroughRepository.save(playthrough);

        Entitlement entitlement =
                Entitlement.builder().user(user).maxUnlockedFloor(1).build();
        entitlementRepository.save(entitlement);
        log.info("Active playthrough created for playerId={}, playthroughId={}", playerId, playthrough.getId());

        return toResponse(playthrough);
    }

    /**
     * Returns the active playthrough for the player if present.
     */
    @Override
    public PlaythroughResponse getActivePlaythrough(UUID playerId) {
        Playthrough playthrough = playthroughRepository
                .findByUserIdAndStatus(playerId, Playthrough.PlaythroughStatus.ACTIVE)
                .orElse(null);

        if (playthrough == null) {
            log.debug("No active playthrough found for playerId={}", playerId);
            return null;
        }

        log.debug("Active playthrough fetched for playerId={}, playthroughId={}", playerId, playthrough.getId());
        return toResponse(playthrough);
    }

    /**
     * Updates player coordinates in the active playthrough.
     */
    @Override
    @Transactional
    public PlaythroughResponse updatePosition(UUID playerId, PositionUpdateRequest request) {
        Playthrough playthrough = playthroughRepository
                .findByUserIdAndStatus(playerId, Playthrough.PlaythroughStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("No active playthrough found"));

        if (!playthrough.getCurrentFloor().getId().equals(request.getFloorId())) {
            log.warn(
                    "Position update rejected: floor mismatch for playerId={}, floorId={}",
                    playerId,
                    request.getFloorId());
            throw new IllegalArgumentException("Floor does not belong to this playthrough");
        }

        playthrough.setPlayerX(request.getPlayerX());
        playthrough.setPlayerY(request.getPlayerY());
        playthrough = playthroughRepository.save(playthrough);
        log.debug("Position updated for playerId={}, x={}, y={}", playerId, request.getPlayerX(), request.getPlayerY());

        return toResponse(playthrough);
    }

    private PlaythroughResponse toResponse(Playthrough playthrough) {
        Floor currentFloor = playthrough.getCurrentFloor();
        return PlaythroughResponse.builder()
                .id(playthrough.getId())
                .currentFloorId(currentFloor != null ? currentFloor.getId() : null)
                .currentFloorNumber(currentFloor != null ? currentFloor.getFloorNumber() : null)
                .playerX(playthrough.getPlayerX())
                .playerY(playthrough.getPlayerY())
                .status(playthrough.getStatus())
                .build();
    }
}
