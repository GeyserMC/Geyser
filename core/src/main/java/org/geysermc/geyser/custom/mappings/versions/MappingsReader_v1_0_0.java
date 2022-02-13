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

package org.geysermc.geyser.custom.mappings.versions;

import com.fasterxml.jackson.databind.JsonNode;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.custom.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.api.custom.items.CustomItemData;
import org.geysermc.geyser.custom.GeyserCustomManager;

import java.io.File;

public class MappingsReader_v1_0_0 extends MappingsReader {
    public MappingsReader_v1_0_0(GeyserCustomManager customManager) {
        super(customManager);
    }

    @Override
    public void readMappings(File file, JsonNode mappingsRoot) {
        this.readItemMappings(file, mappingsRoot);
    }

    @Override
    public void readItemMappings(File file, JsonNode mappingsRoot) {
        JsonNode itemsNode = mappingsRoot.get("items");

        if (itemsNode != null && itemsNode.isObject()) {
            itemsNode.fields().forEachRemaining(entry -> {
                if (entry.getValue().isArray()) {
                    entry.getValue().forEach(data -> {
                        try {
                            CustomItemData customItemData = this.readItemMappingEntry(data);
                            this.customManager.getItemManager().registerCustomItem(entry.getKey(), customItemData);
                        } catch (InvalidCustomMappingsFileException e) {
                            GeyserImpl.getInstance().getLogger().error("Error in custom mapping file: " + file.getName(), e);
                        }
                    });
                }
            });
        }
    }

    @Override
    public CustomItemData readItemMappingEntry(JsonNode node) throws InvalidCustomMappingsFileException {
        if (node == null || !node.isObject()) {
            throw new InvalidCustomMappingsFileException("Invalid item mappings entry");
        }

        if (!node.has("name")) {
            throw new InvalidCustomMappingsFileException("An item entry has no name");
        }
        String name = node.get("name").asText();

        CustomItemData customItemData;
        if (node.has("custom_model_data")) {
            customItemData = new CustomItemData(node.get("custom_model_data").asInt(), name);
        } else if (node.has("damage_predicate")) {
            customItemData = new CustomItemData(node.get("damage_predicate").asDouble(), name);
        } else {
            throw new InvalidCustomMappingsFileException("Item entry " + name + " has no custom model data or damage predicate");
        }

        //The next entries are optional
        if (node.has("display_name")) {
            customItemData.setDisplayName(node.get("display_name").asText());
        }

        if (node.has("is_tool")) {
            customItemData.setIsTool(node.get("is_tool").asBoolean());
        }

        if (node.has("allow_offhand")) {
            customItemData.setAllowOffhand(node.get("allow_offhand").asBoolean());
        }

        if (node.has("is_hat")) {
            customItemData.setIsHat(node.get("is_hat").asBoolean());
        }

        if (node.has("texture_size")) {
            customItemData.setTextureSize(node.get("texture_size").asInt());
        }

        return customItemData;
    }
}
