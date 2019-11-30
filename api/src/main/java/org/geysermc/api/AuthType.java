package org.geysermc.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AuthType {
    OFFLINE("offline"),
    ONLINE("online"),
    FLOODGATE("floodgate");

    public static final AuthType[] VALUES = values();

    private String name;

    public static AuthType getById(int id) {
        return id < VALUES.length ? VALUES[id] : OFFLINE;
    }

    public static AuthType getByName(String name) {
        String lowerCase = name.toLowerCase();
        for (AuthType type : VALUES) {
            if (type.getName().equals(lowerCase)) {
                return type;
            }
        }
        return OFFLINE;
    }
}