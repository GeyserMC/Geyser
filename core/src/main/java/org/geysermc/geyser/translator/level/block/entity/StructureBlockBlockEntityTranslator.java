/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.level.block.entity;

import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.util.StructureBlockUtils;

@BlockEntity(type = BlockEntityType.STRUCTURE_BLOCK)
public class StructureBlockBlockEntityTranslator extends BlockEntityTranslator {

    @Override
    public void translateTag(NbtMapBuilder builder, CompoundTag tag, int blockState) {
        if (tag.size() < 5) {
            return; // These values aren't here
        }

        builder.putString("structureName", getOrDefault(tag.get("name"), ""));

        String mode = getOrDefault(tag.get("mode"), "");
        int bedrockData = switch (mode) {
            case "LOAD" -> 2;
            case "CORNER" -> 3;
            case "DATA" -> 4;
            default -> 1; // SAVE
        };

        builder.putInt("data", bedrockData);
        builder.putString("dataField", ""); // ??? possibly related to Java's "metadata"

        // Mirror behaves different in Java and Bedrock - it requires modifying the position in space as well
        String mirror = getOrDefault(tag.get("mirror"), "");
        byte bedrockMirror = switch (mirror) {
            case "LEFT_RIGHT" -> 1;
            case "FRONT_BACK" -> 2;
            default -> 0; // Or NONE
        };
        builder.putByte("mirror", bedrockMirror);

        builder.putByte("ignoreEntities", getOrDefault(tag.get("ignoreEntities"), (byte) 0));
        builder.putByte("isPowered", getOrDefault(tag.get("powered"), (byte) 0));
        builder.putLong("seed", getOrDefault(tag.get("seed"), 0L));
        builder.putByte("showBoundingBox", getOrDefault(tag.get("showboundingbox"), (byte) 0));

        String rotation = getOrDefault(tag.get("rotation"), "");
        byte bedrockRotation = switch (rotation) {
            case "CLOCKWISE_90" -> 1;
            case "CLOCKWISE_180" -> 2;
            case "COUNTERCLOCKWISE_90" -> 3;
            default -> 0; // Or NONE keep it as 0
        };
        builder.putByte("rotation", bedrockRotation);

        int xStructureSize = getOrDefault(tag.get("sizeX"), 0);
        int zStructureSize = getOrDefault(tag.get("sizeZ"), 0);

        // The "positions" are also offsets on Java
        int posX = getOrDefault(tag.get("posX"), 0);
        int posZ = getOrDefault(tag.get("posZ"), 0);

        Vector3i[] sizeAndOffset = StructureBlockUtils.addOffsets(bedrockRotation, bedrockMirror,
                xStructureSize, getOrDefault(tag.get("sizeY"), 0), zStructureSize,
                posX, getOrDefault(tag.get("posY"), 0), posZ);

        Vector3i offset = sizeAndOffset[0];
        builder.putInt("xStructureOffset", offset.getX());
        builder.putInt("yStructureOffset", offset.getY());
        builder.putInt("zStructureOffset", offset.getZ());

        Vector3i size = sizeAndOffset[1];
        builder.putInt("xStructureSize", size.getX());
        builder.putInt("yStructureSize", size.getY());
        builder.putInt("zStructureSize", size.getZ());

        builder.putFloat("integrity", getOrDefault(tag.get("integrity"), 0f)); // Is 1.0f by default on Java but 100.0f on Bedrock

        // Java's "showair" is unrepresented
    }
}