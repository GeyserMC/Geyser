package org.geysermc.api;

import lombok.Getter;

@Getter
public enum AuthType {
    OFFLINE,
    ONLINE,
    FLOODGATE;

    public static final AuthType[] VALUES = values();

    public static AuthType getById(int id) {
        return id < VALUES.length ? VALUES[id] : OFFLINE;
    }

    public static AuthType getByName(String name) {
        String upperCase = name.toUpperCase();
        for (AuthType type : VALUES) {
            if (type.name().equals(upperCase)) {
                return type;
            }
        }
        return OFFLINE;
    }
}