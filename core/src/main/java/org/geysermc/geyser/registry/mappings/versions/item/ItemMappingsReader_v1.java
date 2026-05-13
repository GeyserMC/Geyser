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

package org.geysermc.geyser.registry.mappings.versions.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.item.custom.CustomRenderOffsets;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.GeyserCustomItemData;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.MappingsReader;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.BiConsumer;

@Deprecated
public class ItemMappingsReader_v1 implements MappingsReader<Identifier, CustomItemDefinition> {

    @Override
    public void read(Path file, JsonObject mappings, BiConsumer<Identifier, CustomItemDefinition> consumer) {
        mappings.entrySet().forEach(entry -> {
            if (entry.getValue() instanceof JsonArray array) {
                array.forEach(data -> {
                    try {
                        Identifier vanillaItemIdentifier = Identifier.of(entry.getKey());
                        CustomItemDefinition customItemData = this.readItemMappingEntry(vanillaItemIdentifier, data);
                        consumer.accept(vanillaItemIdentifier, customItemData);
                    } catch (InvalidCustomMappingsFileException e) {
                        GeyserImpl.getInstance().getLogger().error("Error in registering items for custom mapping file: " + file.toString(), e);
                    }
                });
            }
        });
    }

    private CustomItemDefinition readItemMappingEntry(Identifier identifier, JsonElement element) throws InvalidCustomMappingsFileException {
        if (element == null || !element.isJsonObject()) {
            throw new InvalidCustomMappingsFileException("Invalid item mappings entry");
        }
        JsonObject object = element.getAsJsonObject();

        JsonElement name = object.get("name");
        if (name == null || !name.isJsonPrimitive() || name.getAsString().isEmpty()) {
            throw new InvalidCustomMappingsFileException("An item entry has no name");
        }

        CustomItemData.Builder customItemData = CustomItemData.builder()
            .name(name.getAsString())
            .customItemOptions(this.readItemCustomItemOptions(object));

        //The next entries are optional
        if (object.has("display_name")) {
            customItemData.displayName(object.get("display_name").getAsString());
        }

        if (object.has("icon")) {
            customItemData.icon(object.get("icon").getAsString());
        }

        if (object.has("creative_category")) {
            customItemData.creativeCategory(object.get("creative_category").getAsInt());
        }

        if (object.has("creative_group")) {
            customItemData.creativeGroup(object.get("creative_group").getAsString());
        }

        if (object.has("allow_offhand")) {
            customItemData.allowOffhand(object.get("allow_offhand").getAsBoolean());
        }

        if (object.has("display_handheld")) {
            customItemData.displayHandheld(object.get("display_handheld").getAsBoolean());
        }

        if (object.has("texture_size")) {
            customItemData.textureSize(object.get("texture_size").getAsInt());
        }

        if (object.has("render_offsets")) {
            JsonObject tmpNode = object.getAsJsonObject("render_offsets");

            customItemData.renderOffsets(fromJsonObject(tmpNode));
        }

        if (object.get("tags") instanceof JsonArray tags) {
            Set<String> tagsSet = new ObjectOpenHashSet<>();
            tags.forEach(tag -> tagsSet.add(tag.getAsString()));
            customItemData.tags(tagsSet);
        }

        return ((GeyserCustomItemData) customItemData.build()).toDefinition(identifier).build();
    }

    private CustomItemOptions readItemCustomItemOptions(JsonObject node) {
        CustomItemOptions.Builder customItemOptions = CustomItemOptions.builder();

        JsonElement customModelData = node.get("custom_model_data");
        if (customModelData != null && customModelData.isJsonPrimitive()) {
            customItemOptions.customModelData(customModelData.getAsInt());
        }

        JsonElement damagePredicate = node.get("damage_predicate");
        if (damagePredicate != null && damagePredicate.isJsonPrimitive()) {
            customItemOptions.damagePredicate(damagePredicate.getAsInt());
        }

        JsonElement unbreakable = node.get("unbreakable");
        if (unbreakable != null && unbreakable.isJsonPrimitive()) {
            customItemOptions.unbreakable(unbreakable.getAsBoolean());
        }

        JsonElement defaultItem = node.get("default");
        if (defaultItem != null && defaultItem.isJsonPrimitive()) {
            customItemOptions.defaultItem(defaultItem.getAsBoolean());
        }

        return customItemOptions.build();
    }

    private @Nullable CustomRenderOffsets fromJsonObject(JsonObject node) {
        if (node == null) {
            return null;
        }

        return new CustomRenderOffsets(
            getHandOffsets(node, "main_hand"),
            getHandOffsets(node, "off_hand")
        );
    }

    private CustomRenderOffsets.@Nullable Hand getHandOffsets(JsonObject node, String hand) {
        if (!(node.get(hand) instanceof JsonObject tmpNode)) {
            return null;
        }

        return new CustomRenderOffsets.Hand(
            getPerspectiveOffsets(tmpNode, "first_person"),
            getPerspectiveOffsets(tmpNode, "third_person")
        );
    }

    private CustomRenderOffsets.@Nullable Offset getPerspectiveOffsets(JsonObject node, String perspective) {
        if (!(node.get(perspective) instanceof JsonObject tmpNode)) {
            return null;
        }

        return new CustomRenderOffsets.Offset(
            getOffsetXYZ(tmpNode, "position"),
            getOffsetXYZ(tmpNode, "rotation"),
            getOffsetXYZ(tmpNode, "scale")
        );
    }

    private CustomRenderOffsets.@Nullable OffsetXYZ getOffsetXYZ(JsonObject node, String offsetType) {
        if (!(node.get(offsetType) instanceof JsonObject tmpNode)) {
            return null;
        }

        if (!tmpNode.has("x") || !tmpNode.has("y") || !tmpNode.has("z")) {
            return null;
        }

        return new CustomRenderOffsets.OffsetXYZ(
            tmpNode.get("x").getAsFloat(),
            tmpNode.get("y").getAsFloat(),
            tmpNode.get("z").getAsFloat()
        );
    }
}
