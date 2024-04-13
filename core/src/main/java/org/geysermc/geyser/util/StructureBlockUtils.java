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

package org.geysermc.geyser.util;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureMirror;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureRotation;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureTemplateResponseType;
import org.cloudburstmc.protocol.bedrock.packet.StructureTemplateDataRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.StructureTemplateDataResponsePacket;
import org.geysermc.geyser.session.GeyserSession;

public class StructureBlockUtils {

    private static final NbtMap EMPTY_STRUCTURE_DATA;

    static {
        NbtMapBuilder builder = NbtMap.builder();
        builder.putInt("format_version", 1);
        builder.putCompound("structure", NbtMap.builder()
                .putList("block_indices", NbtType.LIST, NbtList.EMPTY, NbtList.EMPTY)
                .putList("entities", NbtType.COMPOUND)
                .putCompound("palette", NbtMap.EMPTY)
                .build());
        builder.putList("structure_world_origin", NbtType.INT, 0, 0, 0);
        EMPTY_STRUCTURE_DATA = builder.build();
    }

    public static void sendEmptyStructureData(GeyserSession session, StructureTemplateDataRequestPacket packet) {
        StructureTemplateDataResponsePacket responsePacket = new StructureTemplateDataResponsePacket();
        responsePacket.setName(packet.getName());
        responsePacket.setSave(true);
        responsePacket.setTag(EMPTY_STRUCTURE_DATA.toBuilder()
                // Bedrock does not like negative sizes here
                .putList("size", NbtType.INT,
                        packet.getSettings().getSize().getX(),
                        packet.getSettings().getSize().getY(),
                        packet.getSettings().getSize().getZ())
                .build());
        responsePacket.setType(StructureTemplateResponseType.NONE);
        session.sendUpstreamPacket(responsePacket);
    }

    public static void sendStructureData(GeyserSession session,Vector3i size, String name) {
        StructureTemplateDataResponsePacket responsePacket = new StructureTemplateDataResponsePacket();
        responsePacket.setName(name);
        responsePacket.setSave(true);
        responsePacket.setTag(EMPTY_STRUCTURE_DATA.toBuilder()
                // Bedrock does not like negative sizes here
                .putList("size", NbtType.INT, Math.abs(size.getX()), size.getY(), Math.abs(size.getZ()))
                .build());
        responsePacket.setType(StructureTemplateResponseType.QUERY);
        session.sendUpstreamPacket(responsePacket);
    }

    public static Vector3i calculateOffset(StructureRotation structureRotation, StructureMirror structureMirror,
                                           int sizeX, int sizeZ) {
        int newOffsetX = 0;
        int newOffsetZ = 0;

        switch (structureRotation) {
            case ROTATE_90 -> {
                switch (structureMirror) {
                    case NONE -> newOffsetX -= sizeZ - 1;
                    case X -> {
                        newOffsetZ -= sizeZ;
                        newOffsetX -= sizeZ - 1;
                    }
                }
            }
            case ROTATE_180 -> {
                switch (structureMirror) {
                    case NONE -> {
                        newOffsetX -= sizeX - 1;
                        newOffsetZ -= sizeZ - 1;
                    }
                    case Z -> newOffsetX -= sizeX - 1;
                    case X -> newOffsetZ -= sizeZ - 1;
                }
            }
            case ROTATE_270 -> {
                switch (structureMirror) {
                    case NONE -> newOffsetZ -= sizeX - 1;
                    case Z -> {
                        newOffsetZ -= sizeX - 1;
                        newOffsetX -= sizeZ - 1;
                    }
                }
            }
            default -> {
                switch (structureMirror) {
                    case Z -> newOffsetZ -= sizeZ - 1;
                    case X -> newOffsetX -= sizeX - 1;
                }
            }
        }

        return Vector3i.from(newOffsetX, 0, newOffsetZ);
    }
}
