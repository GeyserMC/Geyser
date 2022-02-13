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

package org.geysermc.geyser.custom;

import com.nukkitx.nbt.NbtMap;
import org.geysermc.geyser.api.custom.CustomRenderOffsets;

public record GeyserCustomRenderOffsets(GeyserHand mainHand, GeyserHand offhand) {
    public static GeyserCustomRenderOffsets fromCustomRenderOffsets(CustomRenderOffsets customRenderOffsets) {
        return new GeyserCustomRenderOffsets(GeyserHand.fromHand(customRenderOffsets.mainHand()), GeyserHand.fromHand(customRenderOffsets.offhand()));
    }

    public NbtMap toNbtMap() {
        return NbtMap.builder()
                .putCompound("main_hand", this.mainHand().toNbtMap())
                .putCompound("off_hand", this.offhand().toNbtMap())
                .build();
    }

    public record GeyserHand(GeyserOffset firstPerson, GeyserOffset thirdPerson) {
        public static GeyserHand fromHand(CustomRenderOffsets.Hand hand) {
            return new GeyserHand(GeyserOffset.fromOffset(hand.firstPerson()), GeyserOffset.fromOffset(hand.thirdPerson()));
        }

        public NbtMap toNbtMap() {
            return NbtMap.builder()
                    .putCompound("first_person", this.firstPerson().toNbtMap())
                    .putCompound("third_person", this.thirdPerson().toNbtMap())
                    .build();
        }
    }

    public record GeyserOffset(GeyserOffsetXYZ position, GeyserOffsetXYZ rotation, GeyserOffsetXYZ scale) {
        public static GeyserOffset fromOffset(CustomRenderOffsets.Offset offset) {
            return new GeyserOffset(GeyserOffsetXYZ.fromOffsetXYZ(offset.position()), GeyserOffsetXYZ.fromOffsetXYZ(offset.rotation()), GeyserOffsetXYZ.fromOffsetXYZ(offset.scale()));
        }

        public NbtMap toNbtMap() {
            return NbtMap.builder()
                    .putCompound("position", this.position().toNbtMap())
                    .putCompound("rotation", this.rotation().toNbtMap())
                    .putCompound("scale", this.scale().toNbtMap())
                    .build();
        }
    }

    public record GeyserOffsetXYZ(float x, float y, float z) {
        public static GeyserOffsetXYZ fromOffsetXYZ(CustomRenderOffsets.OffsetXYZ offsetXYZ) {
            return new GeyserOffsetXYZ(offsetXYZ.x(), offsetXYZ.y(), offsetXYZ.z());
        }

        public NbtMap toNbtMap() {
            return NbtMap.builder()
                    .putFloat("x", this.x)
                    .putFloat("y", this.y)
                    .putFloat("z", this.z)
                    .build();
        }
    }
}
