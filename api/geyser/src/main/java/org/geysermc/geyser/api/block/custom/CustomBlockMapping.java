package org.geysermc.geyser.api.block.custom;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.NonNull;

public record CustomBlockMapping(@NonNull CustomBlockData data, @NonNull Map<String, CustomBlockState> states, @NonNull String javaIdentifier, @NonNull boolean overrideItem) {
}
