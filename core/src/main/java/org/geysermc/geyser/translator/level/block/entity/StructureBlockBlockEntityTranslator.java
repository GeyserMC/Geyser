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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureMirror;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureRotation;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.StructureBlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

@BlockEntity(type = BlockEntityType.STRUCTURE_BLOCK)
public class StructureBlockBlockEntityTranslator extends BlockEntityTranslator {
    @Override
    public NbtMap getBlockEntityTag(GeyserSession session, BlockEntityType type, int x, int y, int z, @Nullable NbtMap javaNbt, BlockState blockState) {
        if (javaNbt == null) {
            return super.getBlockEntityTag(session, type, x, y, z, javaNbt, blockState);
        }
        // Sending a structure with size 0 doesn't clear the outline. Hence, we have to force it by replacing the block :/
        int xStructureSize = javaNbt.getInt("sizeX");
        int yStructureSize = javaNbt.getInt("sizeY");
        int zStructureSize = javaNbt.getInt("sizeZ");

        Vector3i size = Vector3i.from(xStructureSize, yStructureSize, zStructureSize);

        if (size.equals(Vector3i.ZERO)) {
            Vector3i position = Vector3i.from(x, y, z);
            String mode = javaNbt.getString("mode");
            
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

        return super.getBlockEntityTag(session, type, x, y, z, javaNbt, blockState);
    }

    @Override
    public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState) {
        if (javaNbt.size() < 5) {
            return; // These values aren't here
        }

        bedrockNbt.putString("structureName", javaNbt.getString("name"));

        String mode = javaNbt.getString("mode");
        int bedrockData = switch (mode) {
            case "LOAD" -> 2;
            case "CORNER" -> 3;
            case "DATA" -> 4;
            default -> 1; // SAVE
        };

        bedrockNbt.putInt("data", bedrockData);
        bedrockNbt.putString("dataField", ""); // ??? possibly related to Java's "metadata"

        // Mirror behaves different in Java and Bedrock - it requires modifying the position in space as well
        String mirror = javaNbt.getString("mirror");
        StructureMirror bedrockMirror = switch (mirror) {
            case "FRONT_BACK" -> StructureMirror.X;
            case "LEFT_RIGHT" -> StructureMirror.Z;
            default -> StructureMirror.NONE;
        };
        bedrockNbt.putByte("mirror", (byte) bedrockMirror.ordinal());

        bedrockNbt.putByte("ignoreEntities", javaNbt.getByte("ignoreEntities"));
        bedrockNbt.putByte("isPowered", javaNbt.getByte("powered"));
        bedrockNbt.putLong("seed", javaNbt.getLong("seed"));
        bedrockNbt.putByte("showBoundingBox", javaNbt.getByte("showboundingbox"));

        String rotation = javaNbt.getString("rotation");
        StructureRotation bedrockRotation = switch (rotation) {
            case "CLOCKWISE_90" -> StructureRotation.ROTATE_90;
            case "CLOCKWISE_180" -> StructureRotation.ROTATE_180;
            case "COUNTERCLOCKWISE_90" -> StructureRotation.ROTATE_270;
            default -> StructureRotation.NONE;
        };
        bedrockNbt.putByte("rotation", (byte) bedrockRotation.ordinal());

        int xStructureSize = javaNbt.getInt("sizeX");
        int yStructureSize = javaNbt.getInt("sizeY");
        int zStructureSize = javaNbt.getInt("sizeZ");

        // The "positions" are also offsets on Java
        int posX = javaNbt.getInt("posX");
        int posY = javaNbt.getInt("posY");
        int posZ = javaNbt.getInt("posZ");

        Vector3i offset = StructureBlockUtils.calculateOffset(bedrockRotation, bedrockMirror,
                xStructureSize, zStructureSize);

        bedrockNbt.putInt("xStructureOffset", posX + offset.getX());
        bedrockNbt.putInt("yStructureOffset", posY);
        bedrockNbt.putInt("zStructureOffset", posZ + offset.getZ());

        bedrockNbt.putInt("xStructureSize", xStructureSize);
        bedrockNbt.putInt("yStructureSize", yStructureSize);
        bedrockNbt.putInt("zStructureSize", zStructureSize);

        bedrockNbt.putFloat("integrity", javaNbt.getFloat("integrity")); // Is 1.0f by default on Java but 100.0f on Bedrock

        // Java's "showair" is unrepresented
    }
}