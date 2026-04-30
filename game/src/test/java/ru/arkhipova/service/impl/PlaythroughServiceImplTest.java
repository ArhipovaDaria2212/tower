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
import ru.arkhipova.exception.ForbiddenException;
import ru.arkhipova.model.entity.Entitlement;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.entity.Playthrough;
import ru.arkhipova.model.entity.User;
import ru.arkhipova.model.request.PositionUpdateRequest;
import ru.arkhipova.repository.EntitlementRepository;
import ru.arkhipova.repository.PlaythroughRepository;
import ru.arkhipova.repository.UserRepository;
import ru.arkhipova.service.FloorService;

@ExtendWith(MockitoExtension.class)
class PlaythroughServiceImplTest {

    @Mock
    private PlaythroughRepository playthroughRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntitlementRepository entitlementRepository;

    @Mock
    private FloorService floorService;

    @InjectMocks
    private PlaythroughServiceImpl playthroughService;

    @Test
    void createPlaythroughReturnsExistingActive() {
        UUID playerId = UUID.randomUUID();
        Playthrough existing = Playthrough.builder()
                .id(UUID.randomUUID())
                .status(Playthrough.PlaythroughStatus.ACTIVE)
                .build();
        when(userRepository.findById(playerId))
                .thenReturn(Optional.of(User.builder().id(playerId).build()));
        when(playthroughRepository.findByUserIdAndStatus(playerId, Playthrough.PlaythroughStatus.ACTIVE))
                .thenReturn(Optional.of(existing));

        var response = playthroughService.createPlaythrough(playerId);

        assertEquals(existing.getId(), response.getId());
        verify(entitlementRepository, never()).save(any(Entitlement.class));
    }

    @Test
    void updatePositionThrowsWhenFloorMismatch() {
        UUID playerId = UUID.randomUUID();
        Floor currentFloor =
                Floor.builder().id(UUID.randomUUID()).floorNumber(1).build();
        Playthrough playthrough = Playthrough.builder()
                .id(UUID.randomUUID())
                .status(Playthrough.PlaythroughStatus.ACTIVE)
                .currentFloor(currentFloor)
                .build();
        PositionUpdateRequest request = PositionUpdateRequest.builder()
                .floorId(UUID.randomUUID())
                .playerX(2f)
                .playerY(3f)
                .build();
        when(playthroughRepository.findByUserIdAndStatus(playerId, Playthrough.PlaythroughStatus.ACTIVE))
                .thenReturn(Optional.of(playthrough));

        assertThrows(ForbiddenException.class, () -> playthroughService.updatePosition(playerId, request));
        verify(playthroughRepository, never()).save(any(Playthrough.class));
    }
}
