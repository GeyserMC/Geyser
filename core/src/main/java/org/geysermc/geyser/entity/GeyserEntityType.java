/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.api.entity.JavaEntityType;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.BuiltinEntityType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public record GeyserEntityType(Identifier javaIdentifier, int javaId) implements JavaEntityType {
    private static final Map<BuiltinEntityType, GeyserEntityType> VANILLA = new EnumMap<>(BuiltinEntityType.class);
    private static final Int2ObjectMap<GeyserEntityType> CUSTOM = new Int2ObjectOpenHashMap<>();

    private GeyserEntityType(BuiltinEntityType builtin) {
        this(Identifier.of(builtin.name().toLowerCase(Locale.ROOT)), builtin.id());
    }

    private GeyserEntityType(int javaId) {
        this(Identifier.of(Constants.GEYSER_CUSTOM_NAMESPACE, "unregistered_sadface"), javaId);
    }

    @Override
    public boolean isUnregistered() {
        return javaIdentifier.namespace().equals(Constants.GEYSER_CUSTOM_NAMESPACE) && javaIdentifier.path().equals("unregistered_sadface");
    }

    public boolean is(EntityType type) {
        return javaId == type.id();
    }

    public static GeyserEntityType ofVanilla(BuiltinEntityType builtin) {
        return VANILLA.computeIfAbsent(builtin, GeyserEntityType::new);
    }

    public static GeyserEntityType of(int javaId) {
        if (javaId >= 0 && javaId < BuiltinEntityType.VALUES.length) {
            return ofVanilla(BuiltinEntityType.VALUES[javaId]);
        }

        GeyserEntityType type = CUSTOM.get(javaId);
        return type == null ? new GeyserEntityType(javaId) : type;
    }

    // TODO improve this
    public static GeyserEntityType of(Key javaKey) {
        Identifier identifier = MinecraftKey.keyToIdentifier(javaKey);
        for (GeyserEntityType builtin : VANILLA.values()) {
            if (builtin.javaIdentifier.equals(identifier)) {
                return builtin;
            }
        }
        for (GeyserEntityType custom : CUSTOM.values()) {
            if (custom.javaIdentifier.equals(identifier)) {
                return custom;
            }
        }
        return null;
    }

    public static GeyserEntityType of(EntityType type) {
        return type instanceof BuiltinEntityType builtin ? ofVanilla(builtin) : of(type.id());
    }
}
