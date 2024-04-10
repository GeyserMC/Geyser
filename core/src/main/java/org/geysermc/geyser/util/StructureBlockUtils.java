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
import org.geysermc.geyser.GeyserImpl;
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
                .putList("size", NbtType.INT, Math.abs(x), y, Math.abs(z))
                .build());
        responsePacket.setType(StructureTemplateResponseType.QUERY);
        session.sendUpstreamPacket(responsePacket);
    }

    public static Vector3i[] removeOffsets(GeyserSession session, StructureSettings newSettings) {
        StructureSettings old = session.getStructureSettings();

        if (old == null) {
            return new Vector3i[] {newSettings.getOffset(), newSettings.getSize()};
        }

        Vector3i offset = old.getOffset();
        Vector3i size = old.getSize().abs();

        int offsetX = offset.getX();
        int offsetZ = offset.getZ();
        int originalSizeX = size.getX();
        int originalSizeZ = size.getZ();

        StructureMirror structureMirror = old.getMirror();
        StructureRotation structureRotation = old.getRotation();

        switch (structureRotation) {
            case ROTATE_90 -> {
                switch (structureMirror) {
                    case NONE -> {
                        //offsetX -= 1;
                    }
                    case X -> {
                        offsetZ -= originalSizeX - 1;
                        offsetX -= 2 * originalSizeX;
                    }
                }
            }
            case ROTATE_180 -> {
                switch (structureMirror) {
                    case NONE -> {
                        offsetX -= 1;
                        offsetZ -= 1;
                        offsetX -= originalSizeX;
                        offsetZ -= originalSizeZ;
                    }
                    case Z -> {
                        offsetX -= originalSizeX - 1;
                    }
                    case X -> {
                        offsetZ -= originalSizeZ - 1;
                    }
                }
            }
            case ROTATE_270 -> {
                switch (structureMirror) {
                    case NONE -> {
                        if (originalSizeX < 0) {
                            offsetX -= 1;
                        }
                        if (originalSizeZ >= 0) {
                            offsetZ -= 1;
                        }
                        offsetZ -= originalSizeX;
                    }
                    case Z -> {
                        offsetZ -= originalSizeX - 1;
                        offsetX -= originalSizeZ - 1;
                    }
                }
            }
            default -> {
                GeyserImpl.getInstance().getLogger().warning(structureMirror.name() + " " + structureRotation.name() + " ");
                switch (structureMirror) {
                    case Z -> {
                        offsetZ = offsetZ - originalSizeZ - 1;
                    }
                    case X -> {
                        offsetX = offsetX - originalSizeX - 1;
                    }
                }
            }
        }

        // These are the size/offsets for the OLD rotation (with our changes for Bedrock undone).
        Vector3i originalOffset = Vector3i.from(offsetX, offset.getY(), offsetZ);
        Vector3i originalSize = Vector3i.from(originalSizeX, size.getY(), originalSizeZ);

        // Now: Add changes done by the player to these
        Vector3i changeOffset = newSettings.getOffset().sub(originalOffset);

        return new Vector3i[]{originalOffset.add(changeOffset), originalSize};
    }

    public static Vector3i[] addOffsets(byte bedrockRotation, byte bedrockMirror,
                                        int sizeX, int sizeY, int sizeZ, int offsetX, int offsetY, int offsetZ) {
        int newXStructureSize = sizeX;
        int newZStructureSize = sizeZ;

        StructureMirror structureMirror = StructureMirror.values()[bedrockMirror];
        StructureRotation structureRotation = StructureRotation.values()[bedrockRotation];

        switch (structureRotation) {
            case ROTATE_90 -> {
                switch (structureMirror) {
                    case NONE -> {
                        //newZStructureSize = sizeZ * -1;
                        offsetZ -= sizeZ; // todo test
                        offsetX += 1;
                        offsetX += sizeZ;
                    }
                    case X -> {
                        newXStructureSize = sizeX * -1;
                        newZStructureSize = sizeZ * -1;
                        offsetZ += sizeX + 1;
                        offsetX += sizeX;
                    }
                }
            }
            case ROTATE_180 -> {
                switch (structureMirror) {
                    case NONE -> {
                        newZStructureSize = sizeZ * -1;
                        newXStructureSize = sizeX * -1;
                        offsetX += sizeX + 1;
                        offsetZ += sizeZ + 1;
                    }
                    case Z -> {
                        newXStructureSize = sizeX * -1;
                        offsetX += sizeX + 1;
                    }
                    case X -> {
                        newZStructureSize = sizeZ * -1;
                        offsetZ += sizeZ + 1;
                    }
                }
            }
            case ROTATE_270 -> {
                switch (structureMirror) {
                    case NONE -> {
                        newXStructureSize = sizeX * -1;
                        offsetZ += sizeX + 1;
                    }
                    case Z -> {
                        newZStructureSize = sizeZ * -1;
                        newXStructureSize = sizeX * -1;
                        offsetZ += sizeX + 1;
                        offsetX += sizeZ + 1;
                    }
                }
            }
            default -> {
                switch (structureMirror) {
                    case Z -> {
                        offsetZ = offsetZ + sizeZ + 1;
                        newZStructureSize = sizeZ * -1;
                    }
                    case X -> {
                        offsetX = offsetX + sizeX + 1;
                        newXStructureSize = sizeX * -1;
                    }
                }
            }
        }

        Vector3i offset = Vector3i.from(offsetX, offsetY, offsetZ);
        Vector3i size = Vector3i.from(newXStructureSize, sizeY, newZStructureSize);

        return new Vector3i[]{offset, size};
    }
}
