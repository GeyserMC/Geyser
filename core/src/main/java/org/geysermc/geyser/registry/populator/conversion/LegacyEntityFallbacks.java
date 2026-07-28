/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.registry.populator.conversion;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.EntityTypeDefinition;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Session-aware Bedrock entity identifier remaps for legacy clients.
 * Aligns with spawn-egg remaps in {@link Legacy120Fallbacks} / {@link Legacy121Fallbacks}.
 */
public final class LegacyEntityFallbacks {

    private LegacyEntityFallbacks() {
    }

    /**
     * Strips entity identifiers unknown to the client's protocol from {@code entity_identifiers.dat}.
     * Sending unsupported ids crashes many legacy Bedrock clients during join.
     */
    public static NbtMap filterIdentifiers(NbtMap identifiers, int protocolVersion) {
        List<NbtMap> idlist = identifiers.getList("idlist", NbtType.COMPOUND);
        List<NbtMap> filtered = new ArrayList<>(idlist.size());
        for (NbtMap entry : idlist) {
            if (clientSupportsEntityType(entry.getString("id"), protocolVersion)) {
                filtered.add(entry);
            }
        }
        if (filtered.size() == idlist.size()) {
            return identifiers;
        }
        return identifiers.toBuilder()
            .putList("idlist", NbtType.COMPOUND, filtered)
            .build();
    }

    /**
     * Remaps the Bedrock definition used at spawn for the given client protocol.
     */
    public static BedrockEntityDefinition remapDefinition(EntityTypeDefinition<?> definition, int protocolVersion) {
        EntityType type = definition.type().mcpl();
        if (type == null) {
            return definition.defaultBedrockDefinition();
        }

        BedrockEntityDefinition substitute = substitute(type, protocolVersion);
        return substitute != null ? substitute : definition.defaultBedrockDefinition();
    }

    /**
     * Remaps a Java entity identifier string (e.g. from spawner NBT) to a Bedrock identifier.
     */
    public static String remapIdentifier(String javaIdentifier, int protocolVersion) {
        if (javaIdentifier == null || javaIdentifier.isEmpty()) {
            return javaIdentifier;
        }

        EntityType type;
        try {
            String path = javaIdentifier.contains(":")
                ? javaIdentifier.substring(javaIdentifier.indexOf(':') + 1)
                : javaIdentifier;
            type = EntityType.valueOf(path.toUpperCase());
        } catch (IllegalArgumentException e) {
            return javaIdentifier.startsWith("minecraft:") ? javaIdentifier : "minecraft:" + javaIdentifier;
        }

        BedrockEntityDefinition substitute = substitute(type, protocolVersion);
        if (substitute != null) {
            return substitute.identifier().toString();
        }

        return javaIdentifier.startsWith("minecraft:") ? javaIdentifier : "minecraft:" + javaIdentifier;
    }

    private static @Nullable BedrockEntityDefinition substitute(EntityType type, int protocolVersion) {
        if (type == EntityType.ARMADILLO && !GameProtocol.is1_20_70orHigher(protocolVersion)) {
            return vanilla("pig");
        }
        if (type == EntityType.BREEZE && !GameProtocol.is1_21_0orHigher(protocolVersion)) {
            return vanilla("blaze");
        }
        if (type == EntityType.BOGGED && !GameProtocol.is1_21_0orHigher(protocolVersion)) {
            return vanilla("stray");
        }
        if ((type == EntityType.BREEZE_WIND_CHARGE || type == EntityType.WIND_CHARGE)
            && !GameProtocol.is1_21_0orHigher(protocolVersion)) {
            return vanilla("snowball");
        }
        // Present on Bedrock from 1.21.0; older clients have no definition.
        if (type == EntityType.OMINOUS_ITEM_SPAWNER && !GameProtocol.is1_21_0orHigher(protocolVersion)) {
            return vanilla("armor_stand");
        }
        if (type == EntityType.CREAKING && !GameProtocol.is1_21_50orHigher(protocolVersion)) {
            return vanilla("warden");
        }
        if (type == EntityType.HAPPY_GHAST && !GameProtocol.is1_21_80orHigher(protocolVersion)) {
            return vanilla("ghast");
        }
        if (type == EntityType.COPPER_GOLEM && !GameProtocol.is1_21_110orHigher(protocolVersion)) {
            return vanilla("iron_golem");
        }
        if ((type == EntityType.NAUTILUS || type == EntityType.ZOMBIE_NAUTILUS)
            && !GameProtocol.is26_0orHigher(protocolVersion)) {
            return vanilla("pufferfish");
        }
        if (type == EntityType.CAMEL_HUSK && !GameProtocol.is26_0orHigher(protocolVersion)) {
            return vanilla("camel");
        }
        if (type == EntityType.PARCHED && !GameProtocol.is26_0orHigher(protocolVersion)) {
            return vanilla("skeleton");
        }
        if (type == EntityType.SULFUR_CUBE && !GameProtocol.is26_10orHigher(protocolVersion)) {
            return vanilla("slime");
        }
        return null;
    }

    /**
     * Whether a SyncEntityProperty "type" identifier is safe to send for this protocol.
     */
    public static boolean clientSupportsEntityType(String bedrockTypeIdentifier, int protocolVersion) {
        if (bedrockTypeIdentifier == null || bedrockTypeIdentifier.isEmpty()) {
            return true;
        }
        String path = pathOf(bedrockTypeIdentifier);
        return switch (path) {
            case "armadillo" -> GameProtocol.is1_20_70orHigher(protocolVersion);
            case "breeze", "bogged", "breeze_wind_charge", "breeze_wind_charge_projectile",
                 "wind_charge", "wind_charge_projectile", "ominous_item_spawner"
                -> GameProtocol.is1_21_0orHigher(protocolVersion);
            // Java-only; Bedrock has no mannequin entity (spawned via AddPlayer instead).
            case "mannequin" -> false;
            case "creaking" -> GameProtocol.is1_21_50orHigher(protocolVersion);
            case "happy_ghast" -> GameProtocol.is1_21_80orHigher(protocolVersion);
            case "copper_golem" -> GameProtocol.is1_21_110orHigher(protocolVersion);
            case "nautilus", "zombie_nautilus", "camel_husk", "parched" -> GameProtocol.is26_0orHigher(protocolVersion);
            case "sulfur_cube" -> GameProtocol.is26_10orHigher(protocolVersion);
            default -> true;
        };
    }

    /**
     * Whether a named Bedrock entity property is safe to register/apply for this protocol.
     * Newer properties on otherwise-supported entities (cow, bee, wolf) crash older clients.
     */
    public static boolean clientSupportsEntityProperty(String propertyName, int protocolVersion) {
        if (propertyName == null || propertyName.isEmpty()) {
            return true;
        }
        return switch (pathOf(propertyName)) {
            // Spring to Life (1.21.70): cow/pig/chicken/egg climate variants
            case "climate_variant" -> GameProtocol.is1_21_70orHigher(protocolVersion);
            default -> true;
        };
    }

    /**
     * Strips unsupported properties from a SyncEntityProperty payload.
     * Returns null when nothing remains to send.
     */
    public static @Nullable NbtMap filterEntityPropertyDefinitions(NbtMap data, int protocolVersion) {
        if (data == null) {
            return null;
        }
        if (!clientSupportsEntityType(data.getString("type"), protocolVersion)) {
            return null;
        }
        List<NbtMap> properties = data.getList("properties", NbtType.COMPOUND);
        if (properties == null || properties.isEmpty()) {
            return data;
        }
        List<NbtMap> filtered = new ArrayList<>(properties.size());
        for (NbtMap property : properties) {
            if (clientSupportsEntityProperty(property.getString("name"), protocolVersion)) {
                filtered.add(property);
            }
        }
        if (filtered.isEmpty()) {
            return null;
        }
        if (filtered.size() == properties.size()) {
            return data;
        }
        return data.toBuilder()
            .putList("properties", NbtType.COMPOUND, filtered)
            .build();
    }

    private static String pathOf(String identifier) {
        return identifier.contains(":")
            ? identifier.substring(identifier.indexOf(':') + 1)
            : identifier;
    }

    private static BedrockEntityDefinition vanilla(String path) {
        return BedrockEntityDefinition.getVanilla(Identifier.of("minecraft:" + path));
    }
}
