package org.geysermc.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PlatformType {

    BUKKIT("Bukkit"),
    BUNGEECORD("BungeeCord"),
    SPONGE("Sponge"),
    STANDALONE("Standalone"),
    VELOCITY("Velocity");

    private String platformName;
}
