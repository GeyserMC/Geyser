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

package org.geysermc.geyser.item.hashing.data;

import net.kyori.adventure.text.BlockNBTComponent;
import net.kyori.adventure.text.EntityNBTComponent;
import net.kyori.adventure.text.NBTComponent;
import net.kyori.adventure.text.StorageNBTComponent;
import org.geysermc.geyser.item.hashing.EnumMapDispatchHasher;
import org.geysermc.geyser.item.hashing.MapBuilder;
import org.geysermc.geyser.item.hashing.MinecraftHasher;

public enum NbtComponentType implements EnumMapDispatchHasher<NbtComponentType, NBTComponent<?>> {
    BLOCK(BlockNBTComponent.class, builder -> builder
        .accept("block", ComponentPosHasher.POS_HASHER, BlockNBTComponent::pos)),
    ENTITY(EntityNBTComponent.class, builder -> builder
        .accept("entity", MinecraftHasher.STRING, component -> component.selector())),
    STORAGE(StorageNBTComponent.class, builder -> builder
        .accept("storage", MinecraftHasher.KEY, StorageNBTComponent::storage));

    public static final MapBuilder<NBTComponent<?>> NBT_COMPONENT_SOURCE_MAP_BUILDER = EnumMapDispatchHasher.dispatch("source", NbtComponentType::values);

    private final Class<? extends NBTComponent<?>> clazz;
    private final MapBuilder<? extends NBTComponent<?>> mapBuilder;

    <T extends NBTComponent<?>> NbtComponentType(Class<T> clazz, MapBuilder<T> mapBuilder) {
        this.clazz = clazz;
        this.mapBuilder = mapBuilder;
    }

    @Override
    public NbtComponentType distinction() {
        return this;
    }

    @Override
    public Class<? extends NBTComponent<?>> valueTypeClass() {
        return clazz;
    }

    @Override
    public MapBuilder<? extends NBTComponent<?>> mapBuilder() {
        return mapBuilder;
    }
}
