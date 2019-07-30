package org.geysermc.connector.network.translators.item;

public enum DyeColor {
    white,
    orange,
    magenta,
    light_blue,
    yellow,
    lime,
    pink,
    gray,
    light_gray,
    cyan,
    purple,
    blue,
    brown,
    green,
    red,
    black;

    public final int id;

    DyeColor() {
        this.id = ordinal();
    }
}
