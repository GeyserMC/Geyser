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

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.protocol.bedrock.data.structure.StructureMirror"
#include "org.cloudburstmc.protocol.bedrock.data.structure.StructureRotation"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.util.StructureBlockUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType"

@BlockEntity(type = BlockEntityType.STRUCTURE_BLOCK)
public class StructureBlockBlockEntityTranslator extends BlockEntityTranslator {
    override public NbtMap getBlockEntityTag(GeyserSession session, BlockEntityType type, int x, int y, int z, NbtMap javaNbt, BlockState blockState) {
        if (javaNbt == null) {
            return super.getBlockEntityTag(session, type, x, y, z, javaNbt, blockState);
        }

        int xStructureSize = javaNbt.getInt("sizeX");
        int yStructureSize = javaNbt.getInt("sizeY");
        int zStructureSize = javaNbt.getInt("sizeZ");

        Vector3i size = Vector3i.from(xStructureSize, yStructureSize, zStructureSize);

        if (size.equals(Vector3i.ZERO)) {
            Vector3i position = Vector3i.from(x, y, z);
            std::string mode = javaNbt.getString("mode");
            

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

    override public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState) {
        if (javaNbt.size() < 5) {
            return;
        }

        bedrockNbt.putString("structureName", javaNbt.getString("name"));

        std::string mode = javaNbt.getString("mode");
        int bedrockData = switch (mode) {
            case "LOAD" -> 2;
            case "CORNER" -> 3;
            case "DATA" -> 4;
            default -> 1;
        };

        bedrockNbt.putInt("data", bedrockData);
        bedrockNbt.putString("dataField", "");


        std::string mirror = javaNbt.getString("mirror");
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

        std::string rotation = javaNbt.getString("rotation");
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

        bedrockNbt.putFloat("integrity", javaNbt.getFloat("integrity"));


    }
}
