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
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureMirror;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureRotation;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.StructureBlockUtils;

@BlockEntity(type = BlockEntityType.STRUCTURE_BLOCK)
public class StructureBlockBlockEntityTranslator extends BlockEntityTranslator {

    @Override
    public NbtMap getBlockEntityTag(GeyserSession session, BlockEntityType type, int x, int y, int z, CompoundTag tag, int blockState) {
        // Sending a structure with size 0 doesn't clear the outline. Hence, we have to force it by replacing the block :/
        int xStructureSize = getOrDefault(tag.get("sizeX"), 0);
        int yStructureSize = getOrDefault(tag.get("sizeY"), 0);
        int zStructureSize = getOrDefault(tag.get("sizeZ"), 0);

        Vector3i size = Vector3i.from(xStructureSize, yStructureSize, zStructureSize);

        if (size.equals(Vector3i.ZERO)) {
            Vector3i position = Vector3i.from(x, y, z);
            String mode = getOrDefault(tag.get("mode"), "");
            
            // Set to air and back to reset the structure block
            UpdateBlockPacket emptyBlockPacket = new UpdateBlockPacket();
            emptyBlockPacket.setDataLayer(0);
            emptyBlockPacket.setBlockPosition(position);
            emptyBlockPacket.setDefinition(session.getBlockMappings().getBedrockAir());
            session.sendUpstreamPacket(emptyBlockPacket);

            UpdateBlockPacket spawnerBlockPacket = new UpdateBlockPacket();
            spawnerBlockPacket.setDataLayer(0);
            spawnerBlockPacket.setBlockPosition(position);
            spawnerBlockPacket.setDefinition(session.getBlockMappings().getStructureBlockFromMode(mode));
            session.sendUpstreamPacket(spawnerBlockPacket);
        }

        return super.getBlockEntityTag(session, type, x, y, z, tag, blockState);
    }

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
        StructureMirror bedrockMirror = switch (mirror) {
            case "FRONT_BACK" -> StructureMirror.X;
            case "LEFT_RIGHT" -> StructureMirror.Z;
            default -> StructureMirror.NONE;
        };
        builder.putByte("mirror", (byte) bedrockMirror.ordinal());

        builder.putByte("ignoreEntities", getOrDefault(tag.get("ignoreEntities"), (byte) 0));
        builder.putByte("isPowered", getOrDefault(tag.get("powered"), (byte) 0));
        builder.putLong("seed", getOrDefault(tag.get("seed"), 0L));
        builder.putByte("showBoundingBox", getOrDefault(tag.get("showboundingbox"), (byte) 0));

        String rotation = getOrDefault(tag.get("rotation"), "");
        StructureRotation bedrockRotation = switch (rotation) {
            case "CLOCKWISE_90" -> StructureRotation.ROTATE_90;
            case "CLOCKWISE_180" -> StructureRotation.ROTATE_180;
            case "COUNTERCLOCKWISE_90" -> StructureRotation.ROTATE_270;
            default -> StructureRotation.NONE;
        };
        builder.putByte("rotation", (byte) bedrockRotation.ordinal());

        int xStructureSize = getOrDefault(tag.get("sizeX"), 0);
        int yStructureSize = getOrDefault(tag.get("sizeY"), 0);
        int zStructureSize = getOrDefault(tag.get("sizeZ"), 0);

        // The "positions" are also offsets on Java
        int posX = getOrDefault(tag.get("posX"), 0);
        int posY = getOrDefault(tag.get("posY"), 0);
        int posZ = getOrDefault(tag.get("posZ"), 0);

        Vector3i offset = StructureBlockUtils.calculateOffset(bedrockRotation, bedrockMirror,
                xStructureSize, zStructureSize);

        builder.putInt("xStructureOffset", posX + offset.getX());
        builder.putInt("yStructureOffset", posY);
        builder.putInt("zStructureOffset", posZ + offset.getZ());

        builder.putInt("xStructureSize", xStructureSize);
        builder.putInt("yStructureSize", yStructureSize);
        builder.putInt("zStructureSize", zStructureSize);

        builder.putFloat("integrity", getOrDefault(tag.get("integrity"), 0f)); // Is 1.0f by default on Java but 100.0f on Bedrock

        // Java's "showair" is unrepresented
    }
}