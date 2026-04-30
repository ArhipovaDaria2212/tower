package ru.arkhipova.model.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;
import ru.arkhipova.model.dto.FogChunkDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FogUpdateRequest {
    @NotNull(message = "Chunks are required")
    private List<FogChunkDto> chunks;
}
