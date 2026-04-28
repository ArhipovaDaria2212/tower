package ru.arkhipova.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.arkhipova.model.dto.DiscoveredIconDto;
import ru.arkhipova.model.entity.Floor;
import ru.arkhipova.repository.FloorRepository;

@ExtendWith(MockitoExtension.class)
class MapIconServiceImplTest {

    @Mock
    private DiscoveredIconRepository discoveredIconRepository;

    @Mock
    private FloorRepository floorRepository;

    @InjectMocks
    private MapIconServiceImpl mapIconService;

    @Test
    void discoverIconReturnsWithoutSaveWhenAlreadyExists() {
        UUID floorId = UUID.randomUUID();
        DiscoveredIconDto request = DiscoveredIconDto.builder().iconId("altar").build();
        when(floorRepository.findById(floorId))
                .thenReturn(Optional.of(Floor.builder().id(floorId).build()));
        when(discoveredIconRepository.existsByFloorIdAndIconId(floorId, "altar"))
                .thenReturn(true);

        var response = mapIconService.discoverIcon(floorId, request);

        assertEquals("altar", response.getIconId());
        verify(discoveredIconRepository, never()).save(any());
    }

    @Test
    void discoverIconsSavesOnlyNewEntries() {
        UUID floorId = UUID.randomUUID();
        when(floorRepository.findById(floorId))
                .thenReturn(Optional.of(Floor.builder().id(floorId).build()));
        when(discoveredIconRepository.existsByFloorIdAndIconId(floorId, "known"))
                .thenReturn(true);
        when(discoveredIconRepository.existsByFloorIdAndIconId(floorId, "new")).thenReturn(false);
        when(discoveredIconRepository.findByFloorId(floorId)).thenReturn(List.of());

        mapIconService.discoverIcons(
                floorId,
                List.of(
                        DiscoveredIconDto.builder().iconId("known").build(),
                        DiscoveredIconDto.builder().iconId("new").build()));

        verify(discoveredIconRepository, times(1)).save(any());
    }
}
