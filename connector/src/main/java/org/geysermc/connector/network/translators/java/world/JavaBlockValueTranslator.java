/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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
        }
        if (packet.getValue() instanceof EndGatewayValue) {
            blockEventPacket.setEventType(1);
        }
        if (packet.getValue() instanceof NoteBlockValue) {
            NoteBlockValueType type = (NoteBlockValueType) packet.getType();

            blockEventPacket.setEventType(type.ordinal());
        }
        if (packet.getValue() instanceof PistonValue) {
            PistonValueType type = (PistonValueType) packet.getType();

            // Unlike everything else, pistons need a block entity packet to convey motion
            // TODO: Doesn't register on chunk load; needs to be interacted with first
            BlockEntityDataPacket blockEntityDataPacket = new BlockEntityDataPacket();
            blockEntityDataPacket.setBlockPosition(Vector3i.from(packet.getPosition().getX(),
                    packet.getPosition().getY(), packet.getPosition().getZ()));
            CompoundTagBuilder builder = CompoundTag.EMPTY.toBuilder();
            builder.intTag("x", packet.getPosition().getX())
                    .intTag("y", packet.getPosition().getY())
                    .intTag("z", packet.getPosition().getZ())
                    .floatTag("Progress", 1.0f)
                    .stringTag("id", "PistonArm")
                    .byteTag("State", (byte) (type.ordinal() - 1));
            blockEntityDataPacket.setData(builder.buildRootTag());
            session.getUpstream().sendPacket(blockEntityDataPacket);
            return;
        }
        if (packet.getValue() instanceof BeaconValue) {
            blockEventPacket.setEventType(1);
        }
        if (packet.getValue() instanceof MobSpawnerValue) {
            blockEventPacket.setEventType(1);
        }
        if (packet.getValue() instanceof EndGatewayValue) {
            blockEventPacket.setEventType(1);
        }

        session.getUpstream().sendPacket(blockEventPacket);
    }
}
