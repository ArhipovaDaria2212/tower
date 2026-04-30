package ru.arkhipova.utils;

import lombok.experimental.UtilityClass;

/**
 * Shared geometry constants for the fog-of-war system.
 *
 * <p>One chunk covers a {@link #CELLS_PER_CHUNK_AXIS}×{@link #CELLS_PER_CHUNK_AXIS} grid in world
 * space, where each cell is one bit of {@link #MASK_SIZE_BYTES} bytes (16×16 = 256 bits = 32 bytes).
 */
@UtilityClass
public final class FogConstants {

    /** World units per chunk on each axis. */
    public static final int CHUNK_SIZE = 16;

    /** Cells per chunk on each axis. */
    public static final int CELLS_PER_CHUNK_AXIS = 16;

    /** Bytes in the bit-mask of a single chunk (16×16 / 8). */
    public static final int MASK_SIZE_BYTES = 32;
}
