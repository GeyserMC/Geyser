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

package org.geysermc.geyser.custom.items;

import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.nukkitx.protocol.bedrock.data.inventory.ComponentItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.custom.items.CustomItemData;
import org.geysermc.geyser.api.custom.items.CustomItemManager;
import org.geysermc.geyser.api.custom.items.CustomItemRegistrationTypes;
import org.geysermc.geyser.api.custom.items.FullyCustomItemData;
import org.geysermc.geyser.custom.GeyserCustomManager;
import org.geysermc.geyser.registry.populator.CustomItemsRegistryPopulator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GeyserCustomItemManager extends CustomItemManager {
    private Map<String, List<CustomItemData>> customMappings = new HashMap<>();

    private List<GeyserCustomItemData> registeredCustomItems = new ArrayList<>();
    private Int2ObjectMap<List<ComponentItemData>> registeredComponentItems = new Int2ObjectOpenHashMap<>();

    private Int2ObjectMap<String> customIdMappings = new Int2ObjectOpenHashMap<>();

    private boolean cantRegisterNewItem() {
        return GeyserImpl.getInstance().isInitialized() || !GeyserImpl.getInstance().getConfig().isAddNonBedrockItems();
    }

    private int nameExists(String name) {
        int nameExists = 0;
        for (String mappingName : this.customIdMappings.values()) {
            String addName = GeyserCustomManager.CUSTOM_PREFIX + name;
            if (Pattern.matches("^" + addName +"(_([0-9])+)?$", mappingName)) {
                nameExists++;
            }
        }
        if (nameExists != 0) {
            GeyserImpl.getInstance().getLogger().warning("Custom item name '" + name + "' already exists and was registered again!");
        }
        return nameExists;
    }

    @Override
    public boolean registerCustomItem(@NotNull String identifier, @NotNull CustomItemData customItemData) {
        if (this.cantRegisterNewItem()) {
            return false;
        }

        GeyserCustomItemData registeredItem = CustomItemsRegistryPopulator.populateRegistry(identifier, customItemData, this.nameExists(customItemData.name()), this.registeredItemCount());

        if (registeredItem.mappingNumber() > 0) {
            this.customMappings.computeIfAbsent(identifier, list -> new ArrayList<>()).add(customItemData);
            this.registeredCustomItems.add(registeredItem);

            for (GeyserCustomItemData.Mapping mapping : registeredItem.getMappings()) {
                this.customIdMappings.put(mapping.integerId(), mapping.stringId());
                GeyserImpl.getInstance().getLogger().info("Registered custom item '" + mapping.stringId() + "' with id " + mapping.integerId());
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean registerCustomItem(@NonNull FullyCustomItemData customItemData) {
        if (this.cantRegisterNewItem()) {
            return false;
        }

        Int2ObjectMap<ComponentItemData> componentItemData = CustomItemsRegistryPopulator.populateRegistry(customItemData, this.registeredItemCount());
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

    public String itemStringFromId(int id) {
        GeyserImpl.getInstance().getLogger().info("Item string from id: " + id + "; " + this.customIdMappings.get(id));
        return this.customIdMappings.get(id);
    }

    public int registeredItemCount() {
        return this.registeredCustomItems.size();
    }

    public List<ComponentItemData> componentItemDataListFromVersion(int protocolVersion) {
        List<ComponentItemData> componentItemDataList = new ArrayList<>();

        for (GeyserCustomItemData registeredItem : this.registeredCustomItems) {
            componentItemDataList.add(registeredItem.getMapping(protocolVersion).componentItemData());
        }

        componentItemDataList.addAll(this.registeredComponentItems.getOrDefault(protocolVersion, new ArrayList<>()));

        return componentItemDataList;
    }

    public List<StartGamePacket.ItemEntry> startGameItemEntryListFromVersion(int protocolVersion) {
        List<StartGamePacket.ItemEntry> itemEntryList = new ArrayList<>();
        for (GeyserCustomItemData registeredItem : this.registeredCustomItems) {
            itemEntryList.add(registeredItem.getMapping(protocolVersion).startGamePacketItemEntry());
        }
        return itemEntryList;
    }

    public static CustomItemRegistrationTypes nbtToRegistrationTypes(CompoundTag nbt) {
        CustomItemRegistrationTypes registrationTypes = new CustomItemRegistrationTypes();

        if (nbt.get("CustomModelData") instanceof IntTag customModelDataTag) {
            registrationTypes.customModelData(customModelDataTag.getValue());
        }

        if (nbt.get("Damage") instanceof IntTag damageTag) {
            registrationTypes.damagePredicate(damageTag.getValue());
        }

        if (nbt.get("Unbreakable") instanceof ByteTag unbreakableTag) {
            registrationTypes.unbreaking(unbreakableTag.getValue() == 1);
        }

        return registrationTypes;
    }
}
