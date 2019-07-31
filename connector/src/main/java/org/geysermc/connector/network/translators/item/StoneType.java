package org.geysermc.connector.network.translators.item;

import lombok.Getter;

public enum StoneType {

    STONE,
    GRANITE,
    POLISHED_GRANITE,
    DIORITE,
    POLISHED_DIORITE,
    ANDESITE,
    POLISHED_ANDESITE;

    @Getter
    private final int id = ordinal();

    public String getName() {
        return name().toLowerCase();
    }
}
