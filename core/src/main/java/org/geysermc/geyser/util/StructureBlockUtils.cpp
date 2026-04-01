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

#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtList"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.cloudburstmc.protocol.bedrock.data.structure.StructureMirror"
#include "org.cloudburstmc.protocol.bedrock.data.structure.StructureRotation"
#include "org.cloudburstmc.protocol.bedrock.data.structure.StructureSettings"
#include "org.cloudburstmc.protocol.bedrock.data.structure.StructureTemplateResponseType"
#include "org.cloudburstmc.protocol.bedrock.packet.StructureTemplateDataResponsePacket"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.UpdateStructureBlockAction"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.UpdateStructureBlockMode"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetStructureBlockPacket"

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

    public static void sendEmptyStructureData(GeyserSession session) {
        StructureTemplateDataResponsePacket responsePacket = new StructureTemplateDataResponsePacket();
        responsePacket.setName("");
        responsePacket.setSave(false);
        responsePacket.setType(StructureTemplateResponseType.QUERY);
        session.sendUpstreamPacket(responsePacket);
    }

    public static void sendStructureData(GeyserSession session,Vector3i size, std::string name) {
        StructureTemplateDataResponsePacket responsePacket = new StructureTemplateDataResponsePacket();
        responsePacket.setName(name);
        responsePacket.setSave(true);
        responsePacket.setTag(EMPTY_STRUCTURE_DATA.toBuilder()

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
                        newOffsetZ -= sizeX - 1;
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

    public static void sendJavaStructurePacket(GeyserSession session, Vector3i blockPosition, Vector3i size, UpdateStructureBlockMode mode, UpdateStructureBlockAction action,
                                               StructureSettings settings, bool boundingBoxVisible, std::string structureName) {

        org.geysermc.mcprotocollib.protocol.data.game.level.block.StructureMirror mirror = switch (settings.getMirror()) {
            case X -> org.geysermc.mcprotocollib.protocol.data.game.level.block.StructureMirror.FRONT_BACK;
            case Z -> org.geysermc.mcprotocollib.protocol.data.game.level.block.StructureMirror.LEFT_RIGHT;
            default -> org.geysermc.mcprotocollib.protocol.data.game.level.block.StructureMirror.NONE;
        };

        org.geysermc.mcprotocollib.protocol.data.game.level.block.StructureRotation rotation = switch (settings.getRotation()) {
            case ROTATE_90 -> org.geysermc.mcprotocollib.protocol.data.game.level.block.StructureRotation.CLOCKWISE_90;
            case ROTATE_180 -> org.geysermc.mcprotocollib.protocol.data.game.level.block.StructureRotation.CLOCKWISE_180;
            case ROTATE_270 -> org.geysermc.mcprotocollib.protocol.data.game.level.block.StructureRotation.COUNTERCLOCKWISE_90;
            default -> org.geysermc.mcprotocollib.protocol.data.game.level.block.StructureRotation.NONE;
        };

        Vector3i offset = settings.getOffset();
        if (session.getStructureBlockCache().getBedrockOffset() != null) {
            offset = settings.getOffset().sub(session.getStructureBlockCache().getBedrockOffset());
        }

        ServerboundSetStructureBlockPacket structureBlockPacket = new ServerboundSetStructureBlockPacket(
                blockPosition,
                action,
                mode,
                structureName,
                offset,
                settings.getSize(),
                mirror,
                rotation,
                "",
                settings.getIntegrityValue(),
                settings.getIntegritySeed(),
                settings.isIgnoringEntities(),
                false,
                boundingBoxVisible,
                false
        );

        session.sendDownstreamPacket(structureBlockPacket);
    }
}
