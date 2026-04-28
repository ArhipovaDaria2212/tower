package ru.arkhipova.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.entity.Playthrough;
import ru.arkhipova.repository.EntitlementRepository;
import ru.arkhipova.repository.FloorRepository;
import ru.arkhipova.repository.PlaythroughRepository;
import ru.arkhipova.service.FogService;

@ExtendWith(MockitoExtension.class)
class FloorServiceImplTest {

    @Mock
    private FloorRepository floorRepository;

    @Mock
    private PlaythroughRepository playthroughRepository;

    @Mock
    private EntitlementRepository entitlementRepository;

    @Mock
    private FogService fogService;

    @InjectMocks
    private FloorServiceImpl floorService;

    @Test
    void advanceFloorBlockedByPaywallWhenNotEntitled() {
        UUID playerId = UUID.randomUUID();
        Floor current = Floor.builder().id(UUID.randomUUID()).floorNumber(1).build();
        Playthrough playthrough = Playthrough.builder()
                .id(UUID.randomUUID())
                .status(Playthrough.PlaythroughStatus.ACTIVE)
                .currentFloor(current)
                .build();

        when(playthroughRepository.findByUserIdAndStatus(playerId, Playthrough.PlaythroughStatus.ACTIVE))
                .thenReturn(Optional.of(playthrough));
        when(entitlementRepository.findByUserId(playerId)).thenReturn(Optional.empty());

        var response = floorService.advanceFloor(playerId);

        assertNull(response.getFloor());
        assertTrue(response.getMessage().contains("requires purchase"));
    }

    @Test
    void createFloorInitializesFog() {
        Playthrough playthrough = Playthrough.builder().id(UUID.randomUUID()).build();
        Floor saved = Floor.builder()
                .id(UUID.randomUUID())
                .playthrough(playthrough)
                .floorNumber(1)
                .build();
        when(floorRepository.save(any(Floor.class))).thenReturn(saved);

        var result = floorService.createFloor(playthrough, 1);

        assertEquals(saved.getId(), result.getId());
        verify(fogService).initializeFog(saved);
    }
}
