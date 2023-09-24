package org.geysermc.geyser.api.block.custom.nonvanilla;

import org.checkerframework.checker.nullness.qual.NonNull;

public record JavaBoundingBox(@NonNull double middleX, @NonNull double middleY, @NonNull double middleZ, @NonNull double sizeX, @NonNull double sizeY, @NonNull double sizeZ) {
}
