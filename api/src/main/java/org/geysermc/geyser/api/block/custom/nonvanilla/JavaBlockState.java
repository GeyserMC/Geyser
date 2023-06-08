package org.geysermc.geyser.api.block.custom.nonvanilla;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.GeyserApi;

public interface JavaBlockState {
    @NonNull String identifier();

    @NonNegative int javaId();

    @NonNegative int stateGroupId();
    
    @NonNegative float blockHardness();

    @NonNull boolean waterlogged();

    @NonNull JavaBoundingBox[] collision();

    @NonNull boolean canBreakWithHand();

    @Nullable String pickItem();

    @Nullable String pistonBehavior();

    @Nullable boolean hasBlockEntity();

    static JavaBlockState.Builder builder() {
        return GeyserApi.api().provider(JavaBlockState.Builder.class);
    }

    interface Builder {
        Builder identifier(@NonNull String identifier);

        Builder javaId(@NonNegative int javaId);

        Builder stateGroupId(@NonNegative int stateGroupId);

        Builder blockHardness(@NonNegative float blockHardness);

        Builder waterlogged(@NonNull boolean waterlogged);

        Builder collision(@NonNull JavaBoundingBox[] collision);

        Builder canBreakWithHand(@NonNull boolean canBreakWithHand);

        Builder pickItem(@Nullable String pickItem);

        Builder pistonBehavior(@Nullable String pistonBehavior);

        Builder hasBlockEntity(@Nullable boolean hasBlockEntity);

        JavaBlockState build();
    }
}
