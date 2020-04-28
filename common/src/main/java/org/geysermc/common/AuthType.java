package org.geysermc.common;

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

    /**
     * Convert the AuthType string (from config) to the enum, OFFLINE on fail
     *
     * @param name AuthType string
     *
     * @return The converted AuthType
     */
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