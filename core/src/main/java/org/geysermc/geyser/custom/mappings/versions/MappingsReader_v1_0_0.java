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
import org.geysermc.geyser.api.custom.items.CustomItemData;
import org.geysermc.geyser.api.custom.items.CustomItemRegistrationTypes;
import org.geysermc.geyser.custom.GeyserCustomManager;
import org.geysermc.geyser.custom.GeyserCustomRenderOffsets;
import org.geysermc.geyser.custom.exception.InvalidCustomMappingsFileException;

import java.nio.file.Path;

public class MappingsReader_v1_0_0 extends MappingsReader {
    public MappingsReader_v1_0_0(GeyserCustomManager customManager) {
        super(customManager);
    }

    @Override
    public void readMappings(Path file, JsonNode mappingsRoot) {
        this.readItemMappings(file, mappingsRoot);
    }

    public void readItemMappings(Path file, JsonNode mappingsRoot) {
        JsonNode itemsNode = mappingsRoot.get("items");

        if (itemsNode != null && itemsNode.isObject()) {
            itemsNode.fields().forEachRemaining(entry -> {
                if (entry.getValue().isArray()) {
                    entry.getValue().forEach(data -> {
                        try {
                            CustomItemData customItemData = this.readItemMappingEntry(data);
                            this.customManager.getItemManager().registerCustomItem(entry.getKey(), customItemData);
                        } catch (InvalidCustomMappingsFileException e) {
                            GeyserImpl.getInstance().getLogger().error("Error in custom mapping file: " + file.toString(), e);
                        }
                    });
                }
            });
        }
    }

    private CustomItemRegistrationTypes readItemRegistrationTypes(JsonNode node) {
        CustomItemRegistrationTypes.Builder registrationTypes = CustomItemRegistrationTypes.builder();

        if (node.has("custom_model_data")) {
            registrationTypes.customModelData(node.get("custom_model_data").asInt());
        }
        if (node.has("damage_predicate")) {
            registrationTypes.damagePredicate(node.get("damage_predicate").asInt());
        }
        if (node.has("unbreaking")) {
            registrationTypes.unbreaking(node.get("unbreaking").asBoolean());
        }

        return registrationTypes.build();
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
        CustomItemData.Builder customItemData = CustomItemData.builder(name, this.readItemRegistrationTypes(node));

        //The next entries are optional
        if (node.has("display_name")) {
            customItemData.displayName(node.get("display_name").asText());
        }

        if (node.has("allow_offhand")) {
            customItemData.allowOffhand(node.get("allow_offhand").asBoolean());
        }

        if (node.has("texture_size")) {
            customItemData.textureSize(node.get("texture_size").asInt());
        }

        if (node.has("render_offsets")) {
            JsonNode tmpNode = node.get("render_offsets");

            customItemData.renderOffsets(GeyserCustomRenderOffsets.fromJsonNode(tmpNode));
        }

        return customItemData.build();
    }
}
