package ru.arkhipova.model.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvanceFloorResponse {
    private FloorResponse floor;
    private String message;
}
