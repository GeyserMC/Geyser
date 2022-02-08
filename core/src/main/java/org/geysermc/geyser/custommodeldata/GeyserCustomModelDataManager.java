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

package org.geysermc.geyser.custommodeldata;

import com.fasterxml.jackson.databind.JsonNode;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.custommodeldata.CustomItemData;
import org.geysermc.geyser.api.custommodeldata.CustomModelDataManager;
import org.geysermc.geyser.custommodeldata.items.CustomItemsRegistryPopulator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeyserCustomModelDataManager extends CustomModelDataManager {
    private Map<String, List<CustomItemData>> customMappings = new HashMap<>();

    @Override
    public void registerCustomItem(String baseItem, CustomItemData customItemData) {
        if (this.customMappings.containsKey(baseItem)) {
            this.customMappings.get(baseItem).add(customItemData);
        } else {
            this.customMappings.put(baseItem, new ArrayList<>(List.of(customItemData)));
        }

        CustomItemsRegistryPopulator.addToRegistry(baseItem, customItemData);
    }

    @Override
    public void loadMappingsFromJson(File file) {
        try {
            JsonNode mappingsRoot = GeyserImpl.JSON_MAPPER.readTree(file);
            JsonNode mappingsDataNode = mappingsRoot.get("items");

            if (mappingsDataNode != null && mappingsDataNode.isObject()) {
                mappingsDataNode.fields().forEachRemaining(entry -> {
                    if (entry.getValue().isObject()) {
                        entry.getValue().fields().forEachRemaining(data -> {
                            CustomItemData customItemData = MappingsConfigReader.readItemMappingEntry(data);
                            this.registerCustomItem(entry.getKey(), customItemData);
                        });
                    }
                });
            }
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger().severe("Failed to read custom item mapping file: " + file.getName(), e);
        }
    }

    public void loadMappingsFromJson() {
        Path customMappingsDirectory = MappingsConfigReader.getCustomMappingsDirectory();
        if (Files.exists(customMappingsDirectory)) {
            try {
                Files.createDirectories(customMappingsDirectory);
            } catch (IOException e) {
                GeyserImpl.getInstance().getLogger().error("Failed to create custom mappings directory", e);
                return;
            }
        }

        File[] mappingsFiles = MappingsConfigReader.getCustomMappingsFiles();
        for (File mappingsFile : mappingsFiles) {
            this.loadMappingsFromJson(mappingsFile);
        }
    }

    @Override
    public List<CustomItemData> getCustomItemData(String baseItem) {
        return this.customMappings.get(baseItem);
    }

    @Override
    public Map<String, List<CustomItemData>> getCustomMappings() {
        return this.customMappings;
    }
}
