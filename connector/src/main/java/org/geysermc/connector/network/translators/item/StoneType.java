package org.geysermc.connector.network.translators.item;

public enum StoneType {
    stone,
    granite,
    polished_granite,
    diorite,
    polished_diorite,
    andesite,
    polished_andesite;

    public final String name;
    public final int id;

    StoneType(String name) {
        this.id = ordinal();
        this.name = name;
    }

    StoneType() {
        this.name = name();
        this.id = ordinal();
    }
}
