package ru.arkhipova.service.impl;

import jakarta.persistence.EntityNotFoundException;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.arkhipova.model.entity.Entitlement;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.entity.Playthrough;
import ru.arkhipova.model.response.AdvanceFloorResponse;
import ru.arkhipova.model.response.FloorResponse;
import ru.arkhipova.repository.EntitlementRepository;
import ru.arkhipova.repository.FloorRepository;
import ru.arkhipova.repository.PlaythroughRepository;
import ru.arkhipova.service.FloorService;
import ru.arkhipova.service.FogService;

@Service
@Slf4j
@RequiredArgsConstructor
public class FloorServiceImpl implements FloorService {

    private final FloorRepository floorRepository;
    private final PlaythroughRepository playthroughRepository;
    private final EntitlementRepository entitlementRepository;
    private final FogService fogService;

    private static final int DEFAULT_GENERATOR_VERSION = 1;
    private static final Random RANDOM = new Random();

    /**
     * Creates a floor for a playthrough and initializes fog data.
     */
    @Override
    @Transactional
    public Floor createFloor(Playthrough playthrough, Integer floorNumber) {
        long seed = generateSeed();

        Floor floor = Floor.builder()
                .playthrough(playthrough)
                .floorNumber(floorNumber)
                .seed(seed)
                .generatorVersion(DEFAULT_GENERATOR_VERSION)
                .build();

        floor = floorRepository.save(floor);

        fogService.initializeFog(floor);
        log.info(
                "Floor created: playthroughId={}, floorNumber={}, floorId={}",
                playthrough.getId(),
                floorNumber,
                floor.getId());

        return floor;
    }

    /**
     * Returns the current floor for the active playthrough.
     */
    @Override
    public FloorResponse getCurrentFloor(UUID playerId) {
        Playthrough playthrough = playthroughRepository
                .findByUserIdAndStatus(playerId, Playthrough.PlaythroughStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("No active playthrough found"));

        Floor floor = playthrough.getCurrentFloor();
        if (floor == null) {
            log.debug("Current floor not set for playerId={}", playerId);
            return null;
        }

        return toResponse(floor);
    }

    /**
     * Advances active playthrough to the next floor with paywall check.
     */
    @Override
    @Transactional
    public AdvanceFloorResponse advanceFloor(UUID playerId) {
        Playthrough playthrough = playthroughRepository
                .findByUserIdAndStatus(playerId, Playthrough.PlaythroughStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("No active playthrough found"));

        int currentFloorNumber = playthrough.getCurrentFloor().getFloorNumber();
        int nextFloorNumber = currentFloorNumber + 1;

        Entitlement entitlement = entitlementRepository.findByUserId(playerId).orElse(null);

        boolean paywallEnabled = isPaywallEnabled(nextFloorNumber);

        if (paywallEnabled) {
            int maxUnlocked = entitlement != null ? entitlement.getMaxUnlockedFloor() : 0;
            if (nextFloorNumber > maxUnlocked) {
                log.warn(
                        "Advance blocked by paywall: playerId={}, requestedFloor={}, maxUnlocked={}",
                        playerId,
                        nextFloorNumber,
                        maxUnlocked);
                return AdvanceFloorResponse.builder()
                        .floor(null)
                        .message("Floor " + nextFloorNumber + " requires purchase")
                        .build();
            }
        }

        Floor nextFloor = createFloor(playthrough, nextFloorNumber);

        playthrough.setCurrentFloor(nextFloor);
        playthroughRepository.save(playthrough);

        if (entitlement != null) {
            entitlement.setMaxUnlockedFloor(nextFloorNumber);
            entitlementRepository.save(entitlement);
        }
        log.info("Advanced floor: playerId={}, floorNumber={}", playerId, nextFloorNumber);

        return AdvanceFloorResponse.builder()
                .floor(toResponse(nextFloor))
                .message("Advanced to floor " + nextFloorNumber)
                .build();
    }

    private long generateSeed() {
        return RANDOM.nextInt(1000000);
    }

    private boolean isPaywallEnabled(Integer floorNumber) {
        return floorNumber >= 2;
    }

    private FloorResponse toResponse(Floor floor) {
        return FloorResponse.builder()
                .id(floor.getId())
                .floorNumber(floor.getFloorNumber())
                .seed(floor.getSeed())
                .generatorVersion(floor.getGeneratorVersion())
                .build();
    }
}
