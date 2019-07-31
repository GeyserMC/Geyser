package org.geysermc.connector.network.translators.item;

public enum WoodType {
    oak,
    spruce,
    birch,
    jungle,
    acacia,
    dark_oak;

    public final int id = ordinal();
}
