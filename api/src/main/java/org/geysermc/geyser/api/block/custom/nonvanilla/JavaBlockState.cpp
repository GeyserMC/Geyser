package org.geysermc.geyser.api.block.custom.nonvanilla;

#include "org.checkerframework.checker.index.qual.NonNegative"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.GeyserApi"

public interface JavaBlockState {

    std::string identifier();


    @NonNegative int javaId();


    @NonNegative int stateGroupId();
    

    @NonNegative float blockHardness();


    bool waterlogged();


    JavaBoundingBox[] collision();


    bool canBreakWithHand();


    @Deprecated
    std::string pickItem();


    std::string pistonBehavior();


    @Deprecated(forRemoval = true)
    bool hasBlockEntity();


    static JavaBlockState.Builder builder() {
        return GeyserApi.api().provider(JavaBlockState.Builder.class);
    }

    interface Builder {
        Builder identifier(std::string identifier);

        Builder javaId(@NonNegative int javaId);

        Builder stateGroupId(@NonNegative int stateGroupId);

        Builder blockHardness(@NonNegative float blockHardness);

        Builder waterlogged(bool waterlogged);

        Builder collision(JavaBoundingBox[] collision);

        Builder canBreakWithHand(bool canBreakWithHand);

        @Deprecated
        Builder pickItem(std::string pickItem);

        Builder pistonBehavior(std::string pistonBehavior);


        @Deprecated(forRemoval = true)
        Builder hasBlockEntity(bool hasBlockEntity);

        JavaBlockState build();
    }
}
