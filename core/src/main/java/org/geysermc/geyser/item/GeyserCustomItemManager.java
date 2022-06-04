/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item;

import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.nukkitx.protocol.bedrock.data.inventory.ComponentItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemManager;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;
import org.geysermc.geyser.item.mappings.MappingsConfigReader;
import org.geysermc.geyser.registry.populator.CustomItemRegistryPopulator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

public class GeyserCustomItemManager extends CustomItemManager {
    public static final String CUSTOM_PREFIX = "geyser_custom:";

    private final Map<String, List<CustomItemData>> customMappings = new HashMap<>();

    private final List<GeyserCustomMappingData> registeredCustomItems = new ArrayList<>();

    public GeyserCustomItemManager() {
        MappingsConfigReader.init(this);
    }

    public void loadMappingsFromJson(BiConsumer<String, CustomItemData> consumer) {
        Path customMappingsDirectory = MappingsConfigReader.getCustomMappingsDirectory();
        if (!Files.exists(customMappingsDirectory)) {
            try {
                Files.createDirectories(customMappingsDirectory);
            } catch (IOException e) {
                GeyserImpl.getInstance().getLogger().error("Failed to create custom mappings directory", e);
                return;
            }
        }

        Path[] mappingsFiles = MappingsConfigReader.getCustomMappingsFiles();
        for (Path mappingsFile : mappingsFiles) {
            MappingsConfigReader.readMappingsFromJson(mappingsFile, consumer);
        }

        if (this.registeredItemCount() != 0) {
            GeyserImpl.getInstance().getLogger().info("Registered " + this.registeredItemCount() + " custom items from mappings");
        }
    }

    @Override
    public @NotNull List<CustomItemData> customItemData(@NotNull String identifier) {
        return this.customMappings.getOrDefault(identifier, Collections.emptyList());
    }

    @Override
    public @NotNull Map<String, List<CustomItemData>> itemMappings() {
        return this.customMappings;
    }

    public int registeredItemCount() {
        return this.registeredCustomItems.size();
    }
}
