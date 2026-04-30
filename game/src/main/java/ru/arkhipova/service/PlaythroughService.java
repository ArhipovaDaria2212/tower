package ru.arkhipova.service;

import java.util.UUID;
import ru.arkhipova.model.request.PositionUpdateRequest;
import ru.arkhipova.model.response.PlaythroughResponse;

public interface PlaythroughService {
    /**
     * Creates a new active playthrough for the player or returns existing one. Entitlement is
     * upserted: if an entitlement row already exists for the user it is reused; new rows are
     * created only on the very first playthrough. The DB also has a UNIQUE(user_id) on
     * {@code entitlements} to defend against races.
     */
    PlaythroughResponse createPlaythrough(UUID playerId);

    /**
     * Returns the active playthrough for the player if present.
     */
    PlaythroughResponse getActivePlaythrough(UUID playerId);

    /**
     * Updates player coordinates in the active playthrough. Defends against:
     * <ul>
     *   <li>Active playthrough with no current floor (NPE on getCurrentFloor().getId()).</li>
     *   <li>NaN/±Infinity coordinates getting persisted into a {@code float} column.</li>
     *   <li>Cross-playthrough writes via mismatched floorId.</li>
     * </ul>
     */
    void updatePosition(UUID playerId, PositionUpdateRequest request);
}
