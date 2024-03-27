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
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureAnimationMode;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureMirror;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureRotation;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureSettings;
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
        if (type == BlockEntityType.STRUCTURE_BLOCK && session.getGameMode() == GameMode.CREATIVE &&
                packet.getPosition().equals(session.getCurrentStructureBlock()) && packet.getNbt() != null && packet.getNbt().size() > 5) {
            CompoundTag map = packet.getNbt();

            String mode = getOrDefault(map.get("mode"), "");
            if (!mode.equalsIgnoreCase("LOAD")) {
                return;
            }

            String mirror = getOrDefault(map.get("mirror"), "");
            byte bedrockMirror = switch (mirror) {
                case "LEFT_RIGHT" -> 1;
                case "FRONT_BACK" -> 2;
                default -> 0; // Or NONE
            };

            String rotation = getOrDefault(map.get("rotation"), "");
            byte bedrockRotation = switch (rotation) {
                case "CLOCKWISE_90" -> 1;
                case "CLOCKWISE_180" -> 2;
                case "COUNTERCLOCKWISE_90" -> 3;
                default -> 0; // Or NONE keep it as 0
            };

            // The "positions" are also offsets on Java
            int posX = getOrDefault(map.get("posX"), 0);
            int posZ = getOrDefault(map.get("posZ"), 0);

            Vector3i[] sizeAndOffset = StructureBlockUtils.addOffsets(bedrockRotation, bedrockMirror,
                    getOrDefault(map.get("sizeX"), 0), getOrDefault(map.get("sizeY"), 0),
                    getOrDefault(map.get("sizeZ"), 0), posX, getOrDefault(map.get("posY"), 0), posZ);

            String name = getOrDefault(map.get("name"), "");

            Vector3i size = sizeAndOffset[1];
            StructureBlockUtils.sendStructureData(session, size.getX(), size.getY(), size.getZ(), name);

            // Create dummy structure settings that store size, offset, mirror and rotation.
            StructureSettings settings = new StructureSettings("",
                    false,
                    false,
                    false,
                    size,
                    sizeAndOffset[0],
                    -1,
                    StructureRotation.from(bedrockRotation),
                    StructureMirror.from(bedrockMirror),
                    StructureAnimationMode.NONE,
                    0, 0, 0, Vector3f.ZERO);
            session.setStructureSettings(settings);
            session.setCurrentStructureBlock(null);
        }
    }


    protected <T> T getOrDefault(Tag tag, T defaultValue) {
        //noinspection unchecked
        return (tag != null && tag.getValue() != null) ? (T) tag.getValue() : defaultValue;
    }
}
