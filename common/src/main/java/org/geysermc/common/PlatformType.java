package org.geysermc.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PlatformType {

    BUNGEECORD("BungeeCord"),
    SPIGOT("Spigot"),
    SPONGE("Sponge"),
    STANDALONE("Standalone"),
    VELOCITY("Velocity");

    private String platformName;
}
