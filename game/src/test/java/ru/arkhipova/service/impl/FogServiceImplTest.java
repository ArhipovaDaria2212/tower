package ru.arkhipova.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.arkhipova.exception.ForbiddenException;
import ru.arkhipova.model.dto.FogChunkDto;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.entity.FogChunk;
import ru.arkhipova.model.entity.Playthrough;
import ru.arkhipova.model.entity.User;
import ru.arkhipova.model.request.FogUpdateRequest;
import ru.arkhipova.repository.FloorRepository;
import ru.arkhipova.repository.FogChunkRepository;

@ExtendWith(MockitoExtension.class)
class FogServiceImplTest {

    @Mock
    private FogChunkRepository fogChunkRepository;

    @Mock
    private FloorRepository floorRepository;

    @InjectMocks
    private FogServiceImpl fogService;

    @Test
    void initializeFogCreatesInitialChunk() {
        Floor floor = Floor.builder().id(UUID.randomUUID()).build();

        fogService.initializeFog(floor);

        ArgumentCaptor<FogChunk> captor = ArgumentCaptor.forClass(FogChunk.class);
        verify(fogChunkRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getChunkX());
        assertEquals(0, captor.getValue().getChunkY());
        assertEquals(32, captor.getValue().getMask().length);
    }

    @Test
    void updateFogCreatesMissingChunk() {
        UUID floorId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        User owner = User.builder().id(playerId).build();
        Playthrough playthrough =
                Playthrough.builder().id(UUID.randomUUID()).user(owner).build();
        Floor floor = Floor.builder().id(floorId).playthrough(playthrough).build();

        byte[] fullMask = new byte[32];
        fullMask[0] = 1;
        fullMask[1] = 2;
        fullMask[2] = 3;
        fullMask[3] = 4;
        String base64 = Base64.getEncoder().encodeToString(fullMask);
        FogChunkDto dto =
                FogChunkDto.builder().chunkX(1).chunkY(2).maskBase64(base64).build();

        when(floorRepository.findById(floorId)).thenReturn(Optional.of(floor));
        when(fogChunkRepository.findByFloorIdAndChunkXAndChunkY(floorId, 1, 2)).thenReturn(Optional.empty());
        when(fogChunkRepository.save(any(FogChunk.class))).thenAnswer(inv -> inv.getArgument(0));

        var saved = fogService.updateFog(
                floorId,
                playerId,
                FogUpdateRequest.builder().chunks(List.of(dto)).build());

        verify(fogChunkRepository).save(any(FogChunk.class));
        assertEquals(1, saved.size());
        assertEquals(base64, saved.get(0).getMaskBase64());
    }

    @Test
    void updateFogRejectsWrongOwner() {
        UUID floorId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        Playthrough playthrough =
                Playthrough.builder().user(User.builder().id(owner).build()).build();
        Floor floor = Floor.builder().id(floorId).playthrough(playthrough).build();
        when(floorRepository.findById(floorId)).thenReturn(Optional.of(floor));

        FogChunkDto dto = FogChunkDto.builder()
                .chunkX(0)
                .chunkY(0)
                .maskBase64(Base64.getEncoder().encodeToString(new byte[32]))
                .build();
        FogUpdateRequest request =
                FogUpdateRequest.builder().chunks(List.of(dto)).build();

        assertThrows(ForbiddenException.class, () -> fogService.updateFog(floorId, intruder, request));
        verify(fogChunkRepository, never()).save(any(FogChunk.class));
    }
}
