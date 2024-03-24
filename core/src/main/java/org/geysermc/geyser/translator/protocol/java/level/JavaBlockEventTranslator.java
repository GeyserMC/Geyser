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

import com.github.steveice10.mc.protocol.data.game.level.block.value.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockEventPacket;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.BlockEventPacket;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.PistonCache;
import org.geysermc.geyser.translator.level.block.entity.PistonBlockEntity;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundBlockEventPacket.class)
public class JavaBlockEventTranslator extends PacketTranslator<ClientboundBlockEventPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundBlockEventPacket packet) {
        Vector3i position = packet.getPosition();
        BlockValue value = packet.getValue();

        BlockEventPacket blockEventPacket = new BlockEventPacket();
        blockEventPacket.setBlockPosition(position);

        if (value instanceof ChestValue chestValue) {
            blockEventPacket.setEventType(1);
            blockEventPacket.setEventData(chestValue.getViewers() > 0 ? 1 : 0);
            session.sendUpstreamPacket(blockEventPacket);
        } else if (value instanceof EndGatewayValue) {
            blockEventPacket.setEventType(1);
            session.sendUpstreamPacket(blockEventPacket);
        } else if (value instanceof NoteBlockValue) {
            session.getGeyser().getWorldManager().getBlockAtAsync(session, position).thenAccept(blockState -> {
                blockEventPacket.setEventData(BlockStateValues.getNoteblockPitch(blockState));
                session.sendUpstreamPacket(blockEventPacket);
            });
        } else if (value instanceof PistonValue pistonValue) {
            PistonValueType action = (PistonValueType) packet.getType();
            Direction direction = Direction.fromPistonValue(pistonValue.getDirection());
            PistonCache pistonCache = session.getPistonCache();

            if (session.getGeyser().getPlatformType() == PlatformType.SPIGOT || session.getErosionHandler().isActive()) {
                // Mostly handled in the GeyserPistonEvents class
                // Retracting sticky pistons is an exception, since the event is not called on Spigot from 1.13.2 - 1.17.1
                // See https://github.com/PaperMC/Paper/blob/6fa1983e9ce177a4a412d5b950fd978620174777/patches/server/0304-Fire-BlockPistonRetractEvent-for-all-empty-pistons.patch
                if (action == PistonValueType.PULLING || action == PistonValueType.CANCELLED_MID_PUSH) {
                    int pistonBlock = session.getGeyser().getWorldManager().getBlockAt(session, position);
                    if (!BlockStateValues.isStickyPiston(pistonBlock)) {
                        return;
                    }
                    if (action != PistonValueType.CANCELLED_MID_PUSH) {
                        Vector3i blockInFrontPos = position.add(direction.getUnitVector());
                        int blockInFront = session.getGeyser().getWorldManager().getBlockAt(session, blockInFrontPos);
                        if (blockInFront != BlockStateValues.JAVA_AIR_ID) {
                            // Piston pulled something
                            return;
                        }
                    }
                    PistonBlockEntity blockEntity = pistonCache.getPistons().computeIfAbsent(position, pos -> new PistonBlockEntity(session, pos, direction, true, true));
                    if (blockEntity.getAction() != action) {
                        blockEntity.setAction(action, Object2IntMaps.emptyMap());
                    }
                }
            } else {
                PistonBlockEntity blockEntity = pistonCache.getPistons().computeIfAbsent(position, pos -> {
                    int blockId = session.getGeyser().getWorldManager().getBlockAt(session, position);
                    boolean sticky = BlockStateValues.isStickyPiston(blockId);
                    boolean extended = action != PistonValueType.PUSHING;
                    return new PistonBlockEntity(session, pos, direction, sticky, extended);
                });
                blockEntity.setAction(action);
            }
        } else if (value instanceof MobSpawnerValue) {
            blockEventPacket.setEventType(1);
            session.sendUpstreamPacket(blockEventPacket);
        } else if (value instanceof BellValue bellValue) {
            // Bells - needed to show ring from other players
            BlockEntityDataPacket blockEntityPacket = new BlockEntityDataPacket();
            blockEntityPacket.setBlockPosition(position);

            NbtMapBuilder builder = NbtMap.builder();
            builder.putInt("x", position.getX());
            builder.putInt("y", position.getY());
            builder.putInt("z", position.getZ());
            builder.putString("id", "Bell");
            int bedrockRingDirection = switch (bellValue.getDirection()) {
                case SOUTH -> 0;
                case WEST -> 1;
                case NORTH -> 2;
                case EAST -> 3;
                default -> throw new IllegalStateException("Unexpected BellValue Direction: " + bellValue.getDirection());
            };
            builder.putInt("Direction", bedrockRingDirection);
            builder.putByte("Ringing", (byte) 1);
            builder.putInt("Ticks", 0);
            
            blockEntityPacket.setData(builder.build());
            session.sendUpstreamPacket(blockEntityPacket);
        }
    }
}
