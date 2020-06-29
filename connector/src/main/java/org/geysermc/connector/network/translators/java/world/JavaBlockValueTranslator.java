/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.world.block.value.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockValuePacket;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.BlockEventPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.world.block.entity.NoteblockBlockEntityTranslator;

import java.util.concurrent.TimeUnit;


@Translator(packet = ServerBlockValuePacket.class)
public class JavaBlockValueTranslator extends PacketTranslator<ServerBlockValuePacket> {

    @Override
    public void translate(ServerBlockValuePacket packet, GeyserSession session) {
        BlockEventPacket blockEventPacket = new BlockEventPacket();
        blockEventPacket.setBlockPosition(Vector3i.from(packet.getPosition().getX(),
                packet.getPosition().getY(), packet.getPosition().getZ()));
        if (packet.getValue() instanceof ChestValue) {
            ChestValue value = (ChestValue) packet.getValue() ;
            blockEventPacket.setEventType(1);
            blockEventPacket.setEventData(value.getViewers() > 0 ? 1 : 0);
            session.sendUpstreamPacket(blockEventPacket);
        }
        if (packet.getValue() instanceof EndGatewayValue) {
            blockEventPacket.setEventType(1);
            session.sendUpstreamPacket(blockEventPacket);
        }
        if (packet.getValue() instanceof NoteBlockValue) {
            NoteblockBlockEntityTranslator.translate(session, packet.getPosition());
            return;
        }
        if (packet.getValue() instanceof PistonValue) {
            PistonValueType type = (PistonValueType) packet.getType();

            // Unlike everything else, pistons need a block entity packet to convey motion
            // TODO: Doesn't register on chunk load; needs to be interacted with first
            Vector3i position = Vector3i.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ());
            if (type == PistonValueType.PUSHING) {
                extendPiston(session, position, 0.0f, 0.0f);
            } else {
                retractPiston(session, position, 1.0f, 1.0f);
            }
        }
        if (packet.getValue() instanceof MobSpawnerValue) {
            blockEventPacket.setEventType(1);
            session.sendUpstreamPacket(blockEventPacket);
        }
        if (packet.getValue() instanceof EndGatewayValue) {
            blockEventPacket.setEventType(1);
            session.sendUpstreamPacket(blockEventPacket);
        }
    }

    /**
     * Emulating a piston extending
     * @param session GeyserSession
     * @param position Block position
     * @param progress How far the piston is
     * @param lastProgress How far the piston last was
     */
    private void extendPiston(GeyserSession session, Vector3i position, float progress, float lastProgress) {
        BlockEntityDataPacket blockEntityDataPacket = new BlockEntityDataPacket();
        blockEntityDataPacket.setBlockPosition(position);
        byte state = (byte) ((progress == 1.0f && lastProgress == 1.0f) ? 2 : 1);
        blockEntityDataPacket.setData(buildPistonTag(position, progress, lastProgress, state));
        session.sendUpstreamPacket(blockEntityDataPacket);
        if (lastProgress != 1.0f) {
            session.getConnector().getGeneralThreadPool().schedule(() ->
                            extendPiston(session, position, (progress >= 1.0f) ? 1.0f : progress + 0.5f, progress),
                    20, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Emulate a piston retracting.
     * @param session GeyserSession
     * @param position Block position
     * @param progress Current progress of piston
     * @param lastProgress Last progress of piston
     */
    private void retractPiston(GeyserSession session, Vector3i position, float progress, float lastProgress) {
        BlockEntityDataPacket blockEntityDataPacket = new BlockEntityDataPacket();
        blockEntityDataPacket.setBlockPosition(position);
        byte state = (byte) ((progress == 0.0f && lastProgress == 0.0f) ? 0 : 3);
        blockEntityDataPacket.setData(buildPistonTag(position, progress, lastProgress, state));
        session.sendUpstreamPacket(blockEntityDataPacket);
        if (lastProgress != 0.0f) {
            session.getConnector().getGeneralThreadPool().schedule(() ->
                            retractPiston(session, position, (progress <= 0.0f) ? 0.0f : progress - 0.5f, progress),
                    20, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Build a piston tag
     * @param position Piston position
     * @param progress Current progress of piston
     * @param lastProgress Last progress of piston
     * @param state
     * @return Bedrock CompoundTag of piston
     */
    private CompoundTag buildPistonTag(Vector3i position, float progress, float lastProgress, byte state) {
        CompoundTagBuilder builder = CompoundTag.EMPTY.toBuilder();
        builder.intTag("x", position.getX())
                .intTag("y", position.getY())
                .intTag("z", position.getZ())
                .floatTag("Progress", progress)
                .floatTag("LastProgress", lastProgress)
                .stringTag("id", "PistonArm")
                .byteTag("NewState", state)
                .byteTag("State", state);
        return builder.buildRootTag();
    }
}
