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
import org.cloudburstmc.protocol.bedrock.data.structure.StructureSettings;
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
        sendStructureData(session, packet.getSettings().getSize().getX(),
                packet.getSettings().getSize().getY(),
                packet.getSettings().getSize().getZ(),
                packet.getName());
    }

    public static void sendStructureData(GeyserSession session, int x, int y, int z, String name) {
        StructureTemplateDataResponsePacket responsePacket = new StructureTemplateDataResponsePacket();
        responsePacket.setName(name);
        responsePacket.setSave(true);
        responsePacket.setTag(EMPTY_STRUCTURE_DATA.toBuilder()
                .putList("size", NbtType.INT, x, y, z)
                .build());
        responsePacket.setType(StructureTemplateResponseType.QUERY);
        session.sendUpstreamPacket(responsePacket);
    }

    public static Vector3i[] getStructureOffsetAndRotation(GeyserSession session, StructureSettings newSettings) {
        StructureSettings old = session.getStructureSettings();

        if (old == null) {
            return new Vector3i[] {newSettings.getOffset(), newSettings.getSize()};
        }

        // Remove offsets for old
        return removeOffset(old.getRotation(), old.getMirror(), old.getOffset(), old.getSize());

        // Remove offsets for new
        //return removeOffset(newSettings.getRotation(), newSettings.getMirror(), temp[0], temp[1]);

    }

    private static Vector3i[] removeOffset(StructureRotation rotation, StructureMirror mirror,
                                                    Vector3i offset, Vector3i size) {
        int sizeX = size.getX();
        int sizeY = size.getY();
        int sizeZ = size.getZ();

        int offsetX = offset.getX();
        int offsetY = offset.getY();
        int offsetZ = offset.getZ();

        // Undo mirror/rotation changes
        switch (rotation) {
            case NONE:
                break;
            case ROTATE_90:
                int tempX = offsetX;
                offsetX = offsetZ;
                offsetZ = sizeX - 1 - tempX;
                int tempSizeX = sizeX;
                sizeX = sizeZ;
                sizeZ = tempSizeX;
                break;
            case ROTATE_180:
                offsetX = sizeX - 1 - offsetX;
                offsetZ = sizeZ - 1 - offsetZ;
                break;
            case ROTATE_270:
                int tempY = offsetY;
                offsetY = offsetZ;
                offsetZ = sizeZ - 1 - tempY;
                int tempSizeZ = sizeZ;
                sizeZ = sizeX;
                sizeX = tempSizeZ;
                break;
        }

        if (mirror == StructureMirror.Z) {
            offsetX = sizeX - 1 - offsetX;
        } else if (mirror == StructureMirror.X) {
            offsetZ = sizeZ - 1 - offsetZ;
        }

        return new Vector3i[]{Vector3i.from(offsetX, offsetY, offsetZ), Vector3i.from(sizeX, sizeY, sizeZ)};
    }

    public static Vector3i[] addOffsets(byte bedrockRotation, byte bedrockMirror,
            int sizeX, int sizeY, int sizeZ, int offsetX, int offsetY, int offsetZ) {
        int newXStructureSize = sizeX;
        int newZStructureSize = sizeZ;

        // Modify positions if mirrored - Bedrock doesn't have this
        if (bedrockMirror == (byte) StructureMirror.Z.ordinal()) {
            offsetX = offsetX + sizeX;
            newXStructureSize = sizeX * -1;
        } else if (bedrockMirror == (byte) StructureMirror.X.ordinal()) {
            offsetZ = offsetZ + sizeZ;
            newZStructureSize = sizeZ * -1;
        }

        // Bedrock rotates with the same origin; Java does not
        StructureRotation structureRotation = StructureRotation.values()[bedrockRotation];
        switch (structureRotation) {
            case ROTATE_90 -> {
                if (sizeX >= 0) {
                    offsetX += 1;
                }
                if (sizeZ < 0) {
                    offsetZ += 1;
                }
                offsetX -= sizeZ;
            }
            case ROTATE_180 -> {
                if (sizeX >= 0) {
                    offsetX += 1;
                }
                if (sizeZ >= 0) {
                    offsetZ += 1;
                }
                offsetX -= sizeX;
                offsetZ -= sizeZ;
            }
            case ROTATE_270 -> {
                if (sizeX < 0) {
                    offsetX += 1;
                }
                if (sizeZ >= 0) {
                    offsetZ += 1;
                }
                offsetZ -= sizeX;
            }
            default -> {
                if (sizeX < 0) {
                    offsetX += 1;
                }
                if (sizeZ < 0) {
                    offsetZ += 1;
                }
            }
        }

        return new Vector3i[]{Vector3i.from(offsetX, offsetY, offsetZ), Vector3i.from(newXStructureSize, sizeY, newZStructureSize)};
    }
}
