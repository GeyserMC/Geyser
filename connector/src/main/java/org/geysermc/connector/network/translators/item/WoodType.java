package org.geysermc.connector.network.translators.item;

import lombok.Getter;

public enum WoodType {

    OAK,
    SPRUCE,
    BIRCH,
    JUNGLE,
    ACACIA,
    DARK_OAK;

    @Getter
    private final int id = ordinal();

    public String getName() {
        return name().toLowerCase();
    }
}
