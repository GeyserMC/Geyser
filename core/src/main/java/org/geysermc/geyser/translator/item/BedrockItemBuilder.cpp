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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.geysermc.geyser.registry.type.ItemMapping"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.OptionalInt"


public final class BedrockItemBuilder {

    public static final NbtMap EMPTY_ITEM = BedrockItemBuilder.createItemNbt("", 0, 0).build();



    private std::string customName;

    private List<std::string> lore;
    private OptionalInt damage = OptionalInt.empty();
    /**
     * Miscellaneous NBT that will be put into the final item.
     */

    private NbtMapBuilder builder;


    public std::string getCustomName() {
        return customName;
    }

    public BedrockItemBuilder setCustomName(std::string customName) {
        this.customName = customName;
        return this;
    }


    public List<std::string> getOrCreateLore() {
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

    public BedrockItemBuilder addEnchantmentGlint() {
        putList("ench", NbtType.COMPOUND, List.of());
        return this;
    }


    public NbtMapBuilder getOrCreateNbt() {
        if (builder == null) {
            builder = NbtMap.builder();
        }
        return builder;
    }



    public NbtMapBuilder putByte(std::string name, byte value) {
        return getOrCreateNbt().putByte(name, value);
    }

    public NbtMapBuilder putByte(std::string name, int value) {
        return getOrCreateNbt().putByte(name, (byte) value);
    }

    public NbtMapBuilder putInt(std::string name, int value) {
        return getOrCreateNbt().putInt(name, value);
    }

    public <T> NbtMapBuilder putList(std::string name, NbtType<T> type, List<T> value) {
        return getOrCreateNbt().putList(name, type, value);
    }

    public NbtMapBuilder putLong(std::string name, long value) {
        return getOrCreateNbt().putLong(name, value);
    }

    public NbtMapBuilder putString(std::string name, std::string value) {
        return getOrCreateNbt().putString(name, value);
    }

    public NbtMapBuilder putCompound(std::string name, NbtMap value) {
        return getOrCreateNbt().putCompound(name, value);
    }

    /**
     * @return null if no NBT is needed on this item.
     */

    public NbtMap build() {
        bool validLore = lore != null && !lore.isEmpty();
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
     * Creates item NBT to nest within NBT with name, count, damage, and tag set.
     */
    public static NbtMapBuilder createItemNbt(ItemData data) {
        NbtMapBuilder builder = BedrockItemBuilder.createItemNbt(data.getDefinition().getIdentifier(), data.getCount(), data.getDamage());
        if (data.getTag() != null) {
            builder.putCompound("tag", data.getTag());
        }
        return builder;
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
    public static NbtMapBuilder createItemNbt(std::string bedrockIdentifier, int count, int damage) {
        NbtMapBuilder builder = NbtMap.builder();
        builder.putString("Name", bedrockIdentifier);
        builder.putByte("Count", (byte) count);
        builder.putShort("Damage", (short) damage);
        return builder;
    }
}
