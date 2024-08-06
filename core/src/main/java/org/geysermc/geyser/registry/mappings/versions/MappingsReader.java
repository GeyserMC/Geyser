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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomRenderOffsets;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.util.CustomBlockMapping;

import java.nio.file.Path;
import java.util.function.BiConsumer;

public abstract class MappingsReader {
    public abstract void readItemMappings(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomItemData> consumer);
    public abstract void readBlockMappings(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomBlockMapping> consumer);

    public abstract CustomItemData readItemMappingEntry(JsonNode node) throws InvalidCustomMappingsFileException;
    public abstract CustomBlockMapping readBlockMappingEntry(String identifier, JsonNode node) throws InvalidCustomMappingsFileException;

    protected @Nullable CustomRenderOffsets fromJsonNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }

        return new CustomRenderOffsets(
                getHandOffsets(node, "main_hand"),
                getHandOffsets(node, "off_hand")
        );
    }

    protected CustomRenderOffsets.@Nullable Hand getHandOffsets(JsonNode node, String hand) {
        JsonNode tmpNode = node.get(hand);
        if (tmpNode == null || !tmpNode.isObject()) {
            return null;
        }

        return new CustomRenderOffsets.Hand(
                getPerspectiveOffsets(tmpNode, "first_person"),
                getPerspectiveOffsets(tmpNode, "third_person")
        );
    }

    protected CustomRenderOffsets.@Nullable Offset getPerspectiveOffsets(JsonNode node, String perspective) {
        JsonNode tmpNode = node.get(perspective);
        if (tmpNode == null || !tmpNode.isObject()) {
            return null;
        }

        return new CustomRenderOffsets.Offset(
                getOffsetXYZ(tmpNode, "position"),
                getOffsetXYZ(tmpNode, "rotation"),
                getOffsetXYZ(tmpNode, "scale")
        );
    }

    protected CustomRenderOffsets.@Nullable OffsetXYZ getOffsetXYZ(JsonNode node, String offsetType) {
        JsonNode tmpNode = node.get(offsetType);
        if (tmpNode == null || !tmpNode.isObject()) {
            return null;
        }

        if (!tmpNode.has("x") || !tmpNode.has("y") || !tmpNode.has("z")) {
            return null;
        }

        return new CustomRenderOffsets.OffsetXYZ(
                tmpNode.get("x").floatValue(),
                tmpNode.get("y").floatValue(),
                tmpNode.get("z").floatValue()
        );
    }
}
