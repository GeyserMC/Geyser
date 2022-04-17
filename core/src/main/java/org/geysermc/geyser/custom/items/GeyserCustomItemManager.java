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
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.custom.items.CustomItemData;
import org.geysermc.geyser.api.custom.items.CustomItemManager;
import org.geysermc.geyser.api.custom.items.CustomItemRegistrationTypes;
import org.geysermc.geyser.custom.GeyserCustomManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GeyserCustomItemManager extends CustomItemManager {
    private Map<String, List<GeyserCustomItemData>> customMappings = new HashMap<>();
    private Int2ObjectMap<String> customIdMappings = new Int2ObjectOpenHashMap<>();

    private int registeredCustomItems = 0;

    @Override
    public void registerCustomItem(@NotNull String baseItem, @NotNull CustomItemData customItemData) {
        int nameExists = 0;
        for (String name : this.customIdMappings.values()) {
            String addName = GeyserCustomManager.CUSTOM_PREFIX + customItemData.name();
            if (Pattern.matches("^" + addName +"(_([0-9])+)?$", name)) {
                nameExists++;
            }
        }
        if (nameExists != 0) {
            GeyserImpl.getInstance().getLogger().warning("Custom item name '" + customItemData.name() + "' already exists and was registered again!");
        }

        GeyserCustomItemData registeredItem = CustomItemsRegistryPopulator.addToRegistry(baseItem, customItemData, nameExists, this);
        if (registeredItem != null && registeredItem.mappings().size() > 0) {
            this.customMappings.computeIfAbsent(baseItem, list -> new ArrayList<>()).add(registeredItem);

            for (GeyserCustomItemData.Mapping mapping : registeredItem.mappings().values()) {
                this.customIdMappings.put(mapping.integerId(), mapping.stringId());
            }

            registeredCustomItems++;
        }
    }

    @Override
    public @NotNull List<CustomItemData> customItemData(@NotNull String baseItem) {
        for (Map.Entry<String, List<GeyserCustomItemData>> entry : this.customMappings.entrySet()) {
            if (entry.getKey().equals(baseItem)) {
                List<CustomItemData> customItemData = new ArrayList<>();
                for (GeyserCustomItemData data : entry.getValue()) {
                    customItemData.add(data.customItemData());
                }
                return customItemData;
            }
        }
        return new ArrayList<>();
    }

    @Override
    public @NotNull Map<String, List<CustomItemData>> customMappings() {
        Map<String, List<CustomItemData>> mappings = new HashMap<>();
        for (Map.Entry<String, List<GeyserCustomItemData>> entry : this.customMappings.entrySet()) {
            List<CustomItemData> customItemData = new ArrayList<>();
            for (GeyserCustomItemData data : entry.getValue()) {
                customItemData.add(data.customItemData());
            }
            mappings.put(entry.getKey(), customItemData);
        }
        return mappings;
    }

    @Override
    public String itemStringFromId(int id) {
        if (this.customIdMappings.containsKey(id)) {
            return this.customIdMappings.get(id);
        } else {
            return null;
        }
    }

    public int registeredItemCount() {
        return this.registeredCustomItems;
    }

    public List<ComponentItemData> componentItemDataListFromVersion(int protocolVersion) {
        List<ComponentItemData> componentItemDataList = new ArrayList<>();
        for (Map.Entry<String, List<GeyserCustomItemData>> entry : this.customMappings.entrySet()) {
            for (GeyserCustomItemData data : entry.getValue()) {
                if (data.mappings().containsKey(protocolVersion)) {
                    componentItemDataList.add(data.mappings().get(protocolVersion).componentItemData());
                }
            }
        }
        return componentItemDataList;
    }

    public List<StartGamePacket.ItemEntry> startGameItemEntryListFromVersion(int protocolVersion) {
        List<StartGamePacket.ItemEntry> itemEntryList = new ArrayList<>();
        for (Map.Entry<String, List<GeyserCustomItemData>> entry : this.customMappings.entrySet()) {
            for (GeyserCustomItemData data : entry.getValue()) {
                if (data.mappings().containsKey(protocolVersion)) {
                    itemEntryList.add(data.mappings().get(protocolVersion).startGamePacketItemEntry());
                }
            }
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
