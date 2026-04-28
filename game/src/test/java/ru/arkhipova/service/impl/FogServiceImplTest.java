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
import ru.arkhipova.model.dto.FogChunkDto;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.model.entity.FogChunk;
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
        Floor floor = Floor.builder().id(floorId).build();
        String base64 = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3, 4});
        FogChunkDto dto =
                FogChunkDto.builder().chunkX(1).chunkY(2).maskBase64(base64).build();

        when(floorRepository.findById(floorId)).thenReturn(Optional.of(floor));
        when(fogChunkRepository.findByFloorIdAndChunkXAndChunkY(floorId, 1, 2)).thenReturn(Optional.empty());

        fogService.updateFog(
                floorId, FogUpdateRequest.builder().chunks(List.of(dto)).build());

        verify(fogChunkRepository).save(any(FogChunk.class));
    }
}
