package org.geysermc.connector.network.translators.item;

import lombok.Getter;

public enum DyeColor {

    WHITE,
    ORANGE,
    MAGENTA,
    LIGHT_BLUE,
    YELLOW,
    LIME,
    PINK,
    GRAY,
    LIGHT_GRAY,
    CYAN,
    PURPLE,
    BLUE,
    BROWN,
    GREEN,
    RED,
    BLACK;

    @Getter
    private final int id = ordinal();

    public String getName() {
        return name().toLowerCase();
    }
}
