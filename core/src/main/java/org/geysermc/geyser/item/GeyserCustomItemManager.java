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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeyserCustomItemManager extends CustomItemManager {
    public static final String CUSTOM_PREFIX = "geyser_custom:";

    private Map<String, List<CustomItemData>> customMappings = new HashMap<>();

    private List<GeyserCustomMappingData> registeredCustomItems = new ArrayList<>();
    private Int2ObjectMap<List<ComponentItemData>> registeredComponentItems = new Int2ObjectOpenHashMap<>();

    public GeyserCustomItemManager() {
        MappingsConfigReader.init(this);
    }

    public void loadMappingsFromJson() {
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
            this.loadMappingsFromJson(mappingsFile);
        }

        if (this.registeredItemCount() != 0) {
            GeyserImpl.getInstance().getLogger().info("Registered " + this.registeredItemCount() + " custom items from mappings");
        }
    }

    public void loadMappingsFromJson(@NotNull Path file) {
        MappingsConfigReader.readMappingsFromJson(file);
    }

    private boolean cantRegisterNewItem() {
        return GeyserImpl.getInstance().isInitialized() || !GeyserImpl.getInstance().getConfig().isAddNonBedrockItems();
    }

    @Override
    public boolean registerCustomItem(@NotNull String identifier, @NotNull CustomItemData customItemData) {
        if (this.cantRegisterNewItem()) {
            return false;
        }

        GeyserCustomMappingData registeredItem = CustomItemRegistryPopulator.populateRegistry(identifier, customItemData, this.registeredItemCount());

        if (registeredItem.mappingNumber() > 0) {
            this.customMappings.computeIfAbsent(identifier, list -> new ArrayList<>()).add(customItemData);
            this.registeredCustomItems.add(registeredItem);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean registerCustomItem(@NonNull NonVanillaCustomItemData customItemData) {
        if (this.cantRegisterNewItem()) {
            return false;
        }

        Int2ObjectMap<ComponentItemData> componentItemData = CustomItemRegistryPopulator.populateRegistry(customItemData, this.registeredItemCount());
        if (componentItemData.size() > 0) {
            for (Int2ObjectMap.Entry<ComponentItemData> entry : componentItemData.int2ObjectEntrySet()) {
                this.registeredComponentItems.computeIfAbsent(entry.getIntKey(), list -> new ArrayList<>()).add(entry.getValue());
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public @NotNull List<CustomItemData> customItemData(@NotNull String identifier) {
        return this.customMappings.getOrDefault(identifier, new ArrayList<>());
    }

    @Override
    public @NotNull Map<String, List<CustomItemData>> itemMappings() {
        return this.customMappings;
    }

    public int registeredItemCount() {
        return this.registeredCustomItems.size();
    }

    public List<ComponentItemData> componentItemDataListFromVersion(int protocolVersion) {
        List<ComponentItemData> componentItemDataList = new ArrayList<>();

        for (GeyserCustomMappingData registeredItem : this.registeredCustomItems) {
            componentItemDataList.add(registeredItem.getMapping(protocolVersion).componentItemData());
        }

        componentItemDataList.addAll(this.registeredComponentItems.getOrDefault(protocolVersion, new ArrayList<>()));

        return componentItemDataList;
    }

    public List<StartGamePacket.ItemEntry> startGameItemEntryListFromVersion(int protocolVersion) {
        List<StartGamePacket.ItemEntry> itemEntryList = new ArrayList<>();
        for (GeyserCustomMappingData registeredItem : this.registeredCustomItems) {
            itemEntryList.add(registeredItem.getMapping(protocolVersion).startGamePacketItemEntry());
        }
        return itemEntryList;
    }

    public static CustomItemOptions nbtToCustomItemOptions(CompoundTag nbt) {
        CustomItemOptions.Builder customItemOptions = CustomItemOptions.builder();

        if (nbt.get("CustomModelData") instanceof IntTag customModelDataTag) {
            customItemOptions.customModelData(customModelDataTag.getValue());
        }

        if (nbt.get("Damage") instanceof IntTag damageTag) {
            customItemOptions.damagePredicate(damageTag.getValue());
        }

        if (nbt.get("Unbreakable") instanceof ByteTag unbreakableTag) {
            customItemOptions.unbreaking(unbreakableTag.getValue() == 1);
        }

        return customItemOptions.build();
    }
}
