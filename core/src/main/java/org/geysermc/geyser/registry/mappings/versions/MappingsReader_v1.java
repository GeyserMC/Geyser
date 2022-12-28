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

package org.geysermc.geyser.registry.mappings.versions;

import com.fasterxml.jackson.databind.JsonNode;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;

import java.nio.file.Path;
import java.util.function.BiConsumer;

public class MappingsReader_v1 extends MappingsReader {
    @Override
    public void readItemMappings(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomItemData> consumer) {
        this.readItemMappingsV1(file, mappingsRoot, consumer);
    }

    @Override
    public void readBlockMappings(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomBlockData> consumer) {
        this.readBlockMappingsV1(file, mappingsRoot, consumer);
    }

    public void readItemMappingsV1(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomItemData> consumer) {
        JsonNode itemsNode = mappingsRoot.get("items");

        if (itemsNode != null && itemsNode.isObject()) {
            itemsNode.fields().forEachRemaining(entry -> {
                if (entry.getValue().isArray()) {
                    entry.getValue().forEach(data -> {
                        try {
                            CustomItemData customItemData = this.readItemMappingEntry(data);
                            consumer.accept(entry.getKey(), customItemData);
                        } catch (InvalidCustomMappingsFileException e) {
                            GeyserImpl.getInstance().getLogger().error("Error in registering items for custom mapping file: " + file.toString(), e);
                        }
                    });
                }
            });
        }
    }

    public void readBlockMappingsV1(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomBlockData> consumer) {
        JsonNode blocksNode = mappingsRoot.get("blocks");

        if (blocksNode != null && blocksNode.isObject()) {
            blocksNode.fields().forEachRemaining(entry -> {
                if (entry.getValue().isObject()) {
                    entry.getValue().forEach(data -> {
                        try {
                            CustomBlockData customBlockData = this.readBlockMappingEntry(data);
                            consumer.accept(entry.getKey(), customBlockData);
                        } catch (InvalidCustomMappingsFileException e) {
                            GeyserImpl.getInstance().getLogger().error("Error in registering blocks for custom mapping file: " + file.toString(), e);
                        }
                    });
                }
            });
        }
    }

    private CustomItemOptions readItemCustomItemOptions(JsonNode node) {
        CustomItemOptions.Builder customItemOptions = CustomItemOptions.builder();

        JsonNode customModelData = node.get("custom_model_data");
        if (customModelData != null && customModelData.isInt()) {
            customItemOptions.customModelData(customModelData.asInt());
        }

        JsonNode damagePredicate = node.get("damage_predicate");
        if (damagePredicate != null && damagePredicate.isInt()) {
            customItemOptions.damagePredicate(damagePredicate.asInt());
        }

        JsonNode unbreakable = node.get("unbreakable");
        if (unbreakable != null && unbreakable.isBoolean()) {
            customItemOptions.unbreakable(unbreakable.asBoolean());
        }

        return customItemOptions.build();
    }

    @Override
    public CustomItemData readItemMappingEntry(JsonNode node) throws InvalidCustomMappingsFileException {
        if (node == null || !node.isObject()) {
            throw new InvalidCustomMappingsFileException("Invalid item mappings entry");
        }

        String name = node.get("name").asText();
        if (name == null || name.isEmpty()) {
            throw new InvalidCustomMappingsFileException("An item entry has no name");
        }

        CustomItemData.Builder customItemData = CustomItemData.builder()
                .name(name)
                .customItemOptions(this.readItemCustomItemOptions(node));

        //The next entries are optional
        if (node.has("display_name")) {
            customItemData.displayName(node.get("display_name").asText());
        }

        if (node.has("icon")) {
            customItemData.icon(node.get("icon").asText());
        }

        if (node.has("allow_offhand")) {
            customItemData.allowOffhand(node.get("allow_offhand").asBoolean());
        }

        if (node.has("texture_size")) {
            customItemData.textureSize(node.get("texture_size").asInt());
        }

        if (node.has("render_offsets")) {
            JsonNode tmpNode = node.get("render_offsets");

            customItemData.renderOffsets(fromJsonNode(tmpNode));
        }

        return customItemData.build();
    }

    @Override
    public CustomBlockData readBlockMappingEntry(JsonNode node) throws InvalidCustomMappingsFileException {
        if (node == null || !node.isObject()) {
            throw new InvalidCustomMappingsFileException("Invalid block mappings entry");
        }

        String name = node.get("name").asText();
        if (name == null || name.isEmpty()) {
            throw new InvalidCustomMappingsFileException("A block entry has no name");
        }

        return null;
    }
}
