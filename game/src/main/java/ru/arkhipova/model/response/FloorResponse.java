package ru.arkhipova.model.response;

import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FloorResponse {
    private UUID id;
    private Integer floorNumber;
    private Long seed;
    private Integer generatorVersion;
}
