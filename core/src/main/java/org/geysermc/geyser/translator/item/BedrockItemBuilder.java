/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.item;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * An intermediary class made to allow easy access to work-in-progress NBT, such as lore and display.
 */
public final class BedrockItemBuilder {
    // All Bedrock-style
    @Nullable
    private String customName;
    @Nullable
    private List<String> lore;
    private OptionalInt damage = OptionalInt.empty();
    /**
     * Miscellaneous NBT that will be put into the final item.
     */
    @Nullable
    private NbtMapBuilder builder;

    @Nullable
    public String getCustomName() {
        return customName;
    }

    public BedrockItemBuilder setCustomName(String customName) {
        this.customName = customName;
        return this;
    }

    @NonNull
    public List<String> getOrCreateLore() {
        if (lore == null) {
            lore = new ArrayList<>();
        }
        return lore;
    }

    public OptionalInt getDamage() {
        return damage;
    }

    public BedrockItemBuilder setDamage(int damage) {
        this.damage = OptionalInt.of(damage);
        return this;
    }

    @NonNull
    public NbtMapBuilder getOrCreateNbt() {
        if (builder == null) {
            builder = NbtMap.builder();
        }
        return builder;
    }

    // NBT convenience methods. Returns NbtMapBuilder since that's what's used the most

    public NbtMapBuilder putByte(String name, byte value) {
        return getOrCreateNbt().putByte(name, value);
    }

    public NbtMapBuilder putByte(String name, int value) {
        return getOrCreateNbt().putByte(name, (byte) value);
    }

    public NbtMapBuilder putInt(String name, int value) {
        return getOrCreateNbt().putInt(name, value);
    }

    public <T> NbtMapBuilder putList(String name, NbtType<T> type, List<T> value) {
        return getOrCreateNbt().putList(name, type, value);
    }

    public NbtMapBuilder putLong(String name, long value) {
        return getOrCreateNbt().putLong(name, value);
    }

    public NbtMapBuilder putString(String name, String value) {
        return getOrCreateNbt().putString(name, value);
    }

    public NbtMapBuilder putCompound(String name, NbtMap value) {
        return getOrCreateNbt().putCompound(name, value);
    }

    /**
     * @return null if no NBT is needed on this item.
     */
    @Nullable
    public NbtMap build() {
        boolean validLore = lore != null && !lore.isEmpty();
        if (customName != null || validLore) {
            NbtMapBuilder display = NbtMap.builder();
            if (customName != null) {
                display.putString("Name", customName);
            }
            if (validLore) {
                display.putList("Lore", NbtType.STRING, lore);
            }
            getOrCreateNbt().put("display", display.build());
        }
        if (damage.isPresent()) {
            getOrCreateNbt().putInt("Damage", damage.getAsInt());
        }
        if (builder == null) {
            return null;
        }
        return builder.build();
    }

    /**
     * Creates item NBT to nest within NBT with name, count, and damage set.
     */
    public static NbtMapBuilder createItemNbt(ItemMapping mapping, int count, int damage) {
        return createItemNbt(mapping.getBedrockIdentifier(), count, damage);
    }

    /**
     * Creates item NBT to nest within NBT with name, count, and damage set.
     */
    public static NbtMapBuilder createItemNbt(String bedrockIdentifier, int count, int damage) {
        NbtMapBuilder builder = NbtMap.builder();
        builder.putString("Name", bedrockIdentifier);
        builder.putByte("Count", (byte) count);
        builder.putShort("Damage", (short) damage);
        return builder;
    }
}
