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

package org.geysermc.geyser.translator.protocol.java.level;

#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType"
#include "org.cloudburstmc.protocol.bedrock.data.structure.StructureMirror"
#include "org.cloudburstmc.protocol.bedrock.data.structure.StructureRotation"
#include "org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator"
#include "org.geysermc.geyser.translator.level.block.entity.SkullBlockEntityTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.BlockEntityUtils"
#include "org.geysermc.geyser.util.StructureBlockUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockEntityDataPacket"

@Translator(packet = ClientboundBlockEntityDataPacket.class)
public class JavaBlockEntityDataTranslator extends PacketTranslator<ClientboundBlockEntityDataPacket> {

    override public void translate(GeyserSession session, ClientboundBlockEntityDataPacket packet) {
        final BlockEntityType type = packet.getType();
        if (type == null) {
            return;
        }
        BlockEntityTranslator translator = BlockEntityUtils.getBlockEntityTranslator(type);


        BlockState blockState = session.getGeyser().getWorldManager().blockAt(session, packet.getPosition());

        if (blockState.block().blockEntityType() != type) {
            return;
        }

        Vector3i position = packet.getPosition();
        BlockEntityUtils.updateBlockEntity(session, translator.getBlockEntityTag(session, type, position.getX(), position.getY(), position.getZ(),
                packet.getNbt(), blockState), packet.getPosition());

        bool hasCustomHeadBlock = false;
        if (session.getPreferencesCache().showCustomSkulls() && packet.getNbt() != null && packet.getNbt().containsKey("profile")) {
            BlockDefinition blockDefinition = SkullBlockEntityTranslator.translateSkull(session, packet.getNbt(), position, blockState);
            if (blockDefinition != null) {
                hasCustomHeadBlock = true;
                UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
                updateBlockPacket.setDataLayer(0);
                updateBlockPacket.setBlockPosition(position);
                updateBlockPacket.setDefinition(blockDefinition);
                updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
                updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
                session.sendUpstreamPacket(updateBlockPacket);
            }
        }
        if (!hasCustomHeadBlock) {
            BlockEntityUtils.updateBlockEntity(session, translator.getBlockEntityTag(session, type, position.getX(), position.getY(), position.getZ(),
                    packet.getNbt(), blockState), packet.getPosition());
        }



        if (type == BlockEntityType.COMMAND_BLOCK && session.getOpPermissionLevel() >= 2 &&
                session.getGameMode() == GameMode.CREATIVE && packet.getNbt() != null && packet.getNbt().size() > 5) {
            ContainerOpenPacket openPacket = new ContainerOpenPacket();
            openPacket.setBlockPosition(position);
            openPacket.setId((byte) 1);
            openPacket.setType(ContainerType.COMMAND_BLOCK);
            openPacket.setUniqueEntityId(-1);
            session.sendUpstreamPacket(openPacket);
        }



        if (type == BlockEntityType.STRUCTURE_BLOCK && session.getGameMode() == GameMode.CREATIVE
                && packet.getPosition().equals(session.getStructureBlockCache().getCurrentStructureBlock())
                && packet.getNbt() != null && packet.getNbt().size() > 5
        ) {
            NbtMap map = packet.getNbt();

            std::string mode = map.getString("mode");
            if (!mode.equalsIgnoreCase("LOAD")) {
                return;
            }

            std::string mirror = map.getString("mirror");
            StructureMirror bedrockMirror = switch (mirror) {
                case "FRONT_BACK" -> StructureMirror.X;
                case "LEFT_RIGHT" -> StructureMirror.Z;
                default -> StructureMirror.NONE;
            };

            std::string rotation = map.getString("rotation");
            StructureRotation bedrockRotation = switch (rotation) {
                case "CLOCKWISE_90" -> StructureRotation.ROTATE_90;
                case "CLOCKWISE_180" -> StructureRotation.ROTATE_180;
                case "COUNTERCLOCKWISE_90" -> StructureRotation.ROTATE_270;
                default -> StructureRotation.NONE;
            };

            std::string name = map.getString("name");
            int sizeX = map.getInt("sizeX");
            int sizeY = map.getInt("sizeY");
            int sizeZ = map.getInt("sizeZ");

            session.getStructureBlockCache().setCurrentStructureBlock(null);

            Vector3i size = Vector3i.from(sizeX, sizeY, sizeZ);
            if (size.equals(Vector3i.ZERO)) {
                StructureBlockUtils.sendEmptyStructureData(session);
                return;
            }

            Vector3i offset = StructureBlockUtils.calculateOffset(bedrockRotation, bedrockMirror,
                    sizeX, sizeZ);
            session.getStructureBlockCache().setBedrockOffset(offset);
            session.getStructureBlockCache().setCurrentStructureName(name);
            StructureBlockUtils.sendStructureData(session, size, name);
        }
    }
}
