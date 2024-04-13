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

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockEntityDataPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureMirror;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureRotation;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.level.block.entity.RequiresBlockState;
import org.geysermc.geyser.translator.level.block.entity.SkullBlockEntityTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.BlockEntityUtils;
import org.geysermc.geyser.util.StructureBlockUtils;

@Translator(packet = ClientboundBlockEntityDataPacket.class)
public class JavaBlockEntityDataTranslator extends PacketTranslator<ClientboundBlockEntityDataPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundBlockEntityDataPacket packet) {
        final BlockEntityType type = packet.getType();
        if (type == null) {
            return;
        }
        BlockEntityTranslator translator = BlockEntityUtils.getBlockEntityTranslator(type);
        // The Java block state is used in BlockEntityTranslator.translateTag() to make up for some inconsistencies
        // between Java block states and Bedrock block entity data
        int blockState;
        if (translator instanceof RequiresBlockState) {
            blockState = session.getGeyser().getWorldManager().getBlockAt(session, packet.getPosition());
        } else {
            blockState = BlockStateValues.JAVA_AIR_ID;
        }

        Vector3i position = packet.getPosition();
        BlockEntityUtils.updateBlockEntity(session, translator.getBlockEntityTag(session, type, position.getX(), position.getY(), position.getZ(),
                packet.getNbt(), blockState), packet.getPosition());
        // Check for custom skulls.
        boolean hasCustomHeadBlock = false;
        if (session.getPreferencesCache().showCustomSkulls() && packet.getNbt() != null && packet.getNbt().contains("SkullOwner")) {
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

        // If block entity is command block, OP permission level is appropriate, player is in creative mode and the NBT is not empty
        // TODO 1.18 re-test
        if (type == BlockEntityType.COMMAND_BLOCK && session.getOpPermissionLevel() >= 2 &&
                session.getGameMode() == GameMode.CREATIVE && packet.getNbt() != null && packet.getNbt().size() > 5) {
            ContainerOpenPacket openPacket = new ContainerOpenPacket();
            openPacket.setBlockPosition(position);
            openPacket.setId((byte) 1);
            openPacket.setType(ContainerType.COMMAND_BLOCK);
            openPacket.setUniqueEntityId(-1);
            session.sendUpstreamPacket(openPacket);
        }

        // When a Java client is trying to load a structure, it expects the server to send it the size of the structure.
        // On 1.20.4, the server does so here - we can pass that through to Bedrock, so we're properly selecting the area.
        if (type == BlockEntityType.STRUCTURE_BLOCK && session.getGameMode() == GameMode.CREATIVE
                && packet.getPosition().equals(session.getStructureBlockCache().getCurrentStructureBlock())
                && packet.getNbt() != null && packet.getNbt().size() > 5
        ) {
            CompoundTag map = packet.getNbt();

            String mode = getOrDefault(map.get("mode"), "");
            if (!mode.equalsIgnoreCase("LOAD")) {
                return;
            }

            String mirror = getOrDefault(map.get("mirror"), "");
            StructureMirror bedrockMirror = switch (mirror) {
                case "FRONT_BACK" -> StructureMirror.X;
                case "LEFT_RIGHT" -> StructureMirror.Z;
                default -> StructureMirror.NONE;
            };

            String rotation = getOrDefault(map.get("rotation"), "");
            StructureRotation bedrockRotation = switch (rotation) {
                case "CLOCKWISE_90" -> StructureRotation.ROTATE_90;
                case "CLOCKWISE_180" -> StructureRotation.ROTATE_180;
                case "COUNTERCLOCKWISE_90" -> StructureRotation.ROTATE_270;
                default -> StructureRotation.NONE;
            };

            String name = getOrDefault(map.get("name"), "");
            int sizeX = getOrDefault(map.get("sizeX"), 0);
            int sizeY = getOrDefault(map.get("sizeY"), 0);
            int sizeZ = getOrDefault(map.get("sizeZ"), 0);

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


    protected <T> T getOrDefault(Tag tag, T defaultValue) {
        //noinspection unchecked
        return (tag != null && tag.getValue() != null) ? (T) tag.getValue() : defaultValue;
    }
}
