/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.bedrock;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.packet.PositionTrackingDBClientRequestPacket;
import com.nukkitx.protocol.bedrock.packet.PositionTrackingDBServerBroadcastPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.LodestoneCache;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.DimensionUtils;

@Translator(packet = PositionTrackingDBClientRequestPacket.class)
public class BedrockPositionTrackingDBClientRequestTranslator extends PacketTranslator<PositionTrackingDBClientRequestPacket> {

    @Override
    public void translate(GeyserSession session, PositionTrackingDBClientRequestPacket packet) {
        PositionTrackingDBServerBroadcastPacket broadcastPacket = new PositionTrackingDBServerBroadcastPacket();
        broadcastPacket.setTrackingId(packet.getTrackingId());

        // Fetch the stored lodestone
        LodestoneCache.LodestonePos pos = session.getLodestoneCache().getPos(packet.getTrackingId());

        // If we don't have data for that ID tell the client its not found
        if (pos == null) {
            broadcastPacket.setAction(PositionTrackingDBServerBroadcastPacket.Action.NOT_FOUND);
            session.sendUpstreamPacket(broadcastPacket);
            return;
        }

        broadcastPacket.setAction(PositionTrackingDBServerBroadcastPacket.Action.UPDATE);

        // Build the NBT data for the update
        NbtMapBuilder builder = NbtMap.builder();
        builder.putInt("dim", DimensionUtils.javaToBedrock(pos.getDimension()));
        builder.putString("id", "0x" + String.format("%08X", packet.getTrackingId()));

        builder.putByte("version", (byte) 1); // Not sure what this is for
        builder.putByte("status", (byte) 0); // Not sure what this is for

        // Build the position for the update
        builder.putList("pos", NbtType.INT, pos.getX(), pos.getY(), pos.getZ());
        broadcastPacket.setTag(builder.build());

        session.sendUpstreamPacket(broadcastPacket);
    }
}
