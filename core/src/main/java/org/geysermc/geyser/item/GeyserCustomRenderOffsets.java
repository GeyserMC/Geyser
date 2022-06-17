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

import com.fasterxml.jackson.databind.JsonNode;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.geysermc.geyser.api.item.custom.CustomRenderOffsets;

import java.util.List;

public record GeyserCustomRenderOffsets(GeyserHand mainHand, GeyserHand offhand) {
    public static GeyserCustomRenderOffsets fromCustomRenderOffsets(CustomRenderOffsets customRenderOffsets) {
        if (customRenderOffsets == null) {
            return null;
        }

        return new GeyserCustomRenderOffsets(GeyserHand.fromHand(customRenderOffsets.mainHand()), GeyserHand.fromHand(customRenderOffsets.offhand()));
    }

    public NbtMap toNbtMap() {
        NbtMap mainHand = null;
        if (this.mainHand != null) {
            mainHand = this.mainHand.toNbtMap();
        }
        NbtMap offhand = null;
        if (this.offhand != null) {
            offhand = this.offhand.toNbtMap();
        }

        NbtMapBuilder builder = NbtMap.builder();
        if (mainHand != null) {
            builder.putCompound("main_hand", mainHand);
        }
        if (offhand != null) {
            builder.putCompound("off_hand", offhand);
        }

        return builder.build();
    }

    public record GeyserHand(GeyserOffset firstPerson, GeyserOffset thirdPerson) {
        public static GeyserHand fromHand(CustomRenderOffsets.Hand hand) {
            if (hand == null) {
                return null;
            }

            return new GeyserHand(GeyserOffset.fromOffset(hand.firstPerson()), GeyserOffset.fromOffset(hand.thirdPerson()));
        }

        public NbtMap toNbtMap() {
            NbtMap firstPerson = null;
            if (this.firstPerson != null) {
                firstPerson = this.firstPerson.toNbtMap();
            }
            NbtMap thirdPerson = null;
            if (this.thirdPerson != null) {
                thirdPerson = this.thirdPerson.toNbtMap();
            }

            if (firstPerson == null && thirdPerson == null) {
                return null;
            }

            NbtMapBuilder builder = NbtMap.builder();
            if (firstPerson != null) {
                builder.putCompound("first_person", firstPerson);
            }
            if (thirdPerson != null) {
                builder.putCompound("third_person", thirdPerson);
            }

            return builder.build();
        }
    }

    public record GeyserOffset(GeyserOffsetXYZ position, GeyserOffsetXYZ rotation, GeyserOffsetXYZ scale) {
        public static GeyserOffset fromOffset(CustomRenderOffsets.Offset offset) {
            if (offset == null) {
                return null;
            }

            return new GeyserOffset(GeyserOffsetXYZ.fromOffsetXYZ(offset.position()), GeyserOffsetXYZ.fromOffsetXYZ(offset.rotation()), GeyserOffsetXYZ.fromOffsetXYZ(offset.scale()));
        }

        public NbtMap toNbtMap() {
            List<Float> position = null;
            if (this.position != null) {
                position = this.position.toList();
            }
            List<Float> rotation = null;
            if (this.rotation != null) {
                rotation = this.rotation.toList();
            }
            List<Float> scale = null;
            if (this.scale != null) {
                scale = this.scale.toList();
            }

            if (position == null && rotation == null && scale == null) {
                return null;
            }

            NbtMapBuilder builder = NbtMap.builder();
            if (position != null) {
                builder.putList("position", NbtType.FLOAT, position);
            }
            if (rotation != null) {
                builder.putList("rotation", NbtType.FLOAT, rotation);
            }
            if (scale != null) {
                builder.putList("scale", NbtType.FLOAT, scale);
            }

            return builder.build();
        }
    }

    public record GeyserOffsetXYZ(float x, float y, float z) {
        public static GeyserOffsetXYZ fromOffsetXYZ(CustomRenderOffsets.OffsetXYZ offsetXYZ) {
            if (offsetXYZ == null) {
                return null;
            }

            return new GeyserOffsetXYZ(offsetXYZ.x(), offsetXYZ.y(), offsetXYZ.z());
        }

        public List<Float> toList() {
            return List.of(x, y, z);
        }
    }

    public static CustomRenderOffsets fromJsonNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }

        return new CustomRenderOffsets(
                getHandOffsets(node, "main_hand"),
                getHandOffsets(node, "off_hand")
        );
    }

    private static CustomRenderOffsets.Hand getHandOffsets(JsonNode node, String hand) {
        JsonNode tmpNode = node.get(hand);
        if (tmpNode == null || !tmpNode.isObject()) {
            return null;
        }

        return new CustomRenderOffsets.Hand(
                getPerspectiveOffsets(tmpNode, "first_person"),
                getPerspectiveOffsets(tmpNode, "third_person")
        );
    }

    private static CustomRenderOffsets.Offset getPerspectiveOffsets(JsonNode node, String perspective) {
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

    private static CustomRenderOffsets.OffsetXYZ getOffsetXYZ(JsonNode node, String offsetType) {
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
