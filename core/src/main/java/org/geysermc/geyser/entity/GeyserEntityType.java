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

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.definition.JavaEntityType;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public record GeyserEntityType(Identifier identifier, EntityType mcpl) implements JavaEntityType {
    private static final Identifier UNREGISTERED = Identifier.of(Constants.GEYSER_CUSTOM_NAMESPACE, "unregistered_sadface");

    private static final Map<EntityType, GeyserEntityType> VANILLA = new EnumMap<>(EntityType.class);

    private GeyserEntityType(EntityType builtin) {
        this(Identifier.of(builtin.name().toLowerCase(Locale.ROOT)), builtin);
    }

    public boolean isUnregistered() {
        return identifier.equals(UNREGISTERED);
    }

    @Override
    public boolean vanilla() {
        return true;
    }

    @Override
    public float width() {
        var definition = Registries.JAVA_ENTITY_TYPES.get(this);
        if (definition == null) {
            throw new IllegalStateException("No entity definition registered for " + this);
        }
        return definition.width();
    }

    @Override
    public float height() {
        var definition = Registries.JAVA_ENTITY_TYPES.get(this);
        if (definition == null) {
            throw new IllegalStateException("No entity definition registered for " + this);
        }
        return definition.height();
    }

    @Override
    public @Nullable GeyserEntityDefinition defaultBedrockDefinition() {
        var definition = Registries.JAVA_ENTITY_TYPES.get(this);
        if (definition == null) {
            throw new IllegalStateException("No entity definition registered for " + this);
        }
        return definition.defaultBedrockDefinition();
    }

    public boolean is(EntityType type) {
        return this.mcpl == type;
    }

    public static GeyserEntityType ofVanilla(EntityType builtin) {
        return VANILLA.computeIfAbsent(builtin, GeyserEntityType::new);
    }

    public static GeyserEntityType ofVanilla(Identifier javaIdentifier) {
        return ofVanilla(EntityType.valueOf(javaIdentifier.path().toUpperCase(Locale.ROOT)));
    }

    public static GeyserEntityType of(int javaId) {
        return ofVanilla(EntityType.from(javaId));
    }

    @Nullable
    public static GeyserEntityType of(Key javaKey) {
        if (javaKey.namespace().equals(Key.MINECRAFT_NAMESPACE)) {
            try {
                return ofVanilla(MinecraftKey.keyToIdentifier(javaKey));
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
        throw new IllegalArgumentException("Unsupported mcpl: " + javaKey.getClass().getName());
    }

    public static GeyserEntityType of(EntityType type) {
        return ofVanilla(type);
    }
}
