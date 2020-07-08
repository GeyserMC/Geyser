/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.bedrock;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.packet.PositionTrackingDBClientRequestPacket;
import com.nukkitx.protocol.bedrock.packet.PositionTrackingDBServerBroadcastPacket;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.DimensionUtils;
import org.geysermc.connector.utils.LoadstoneTracker;

@Translator(packet = PositionTrackingDBClientRequestPacket.class)
public class BedrockPositionTrackingDBClientRequestTranslator extends PacketTranslator<PositionTrackingDBClientRequestPacket> {

    @Override
    public void translate(PositionTrackingDBClientRequestPacket packet, GeyserSession session) {
        PositionTrackingDBServerBroadcastPacket broadcastPacket = new PositionTrackingDBServerBroadcastPacket();
        broadcastPacket.setTrackingId(packet.getTrackingId());

        // Fetch the stored Loadstone
        LoadstoneTracker.LoadstonePos pos = LoadstoneTracker.getPos(packet.getTrackingId());

        // If we don't have data for that ID tell the client its not found
        if (pos == null) {
            broadcastPacket.setAction(PositionTrackingDBServerBroadcastPacket.Action.NOT_FOUND);
            session.sendUpstreamPacket(broadcastPacket);
            return;
        }

        broadcastPacket.setAction(PositionTrackingDBServerBroadcastPacket.Action.UPDATE);

        // Build the nbt data for the update
        NbtMapBuilder builder = NbtMap.builder();
        builder.putInt("dim", DimensionUtils.javaToBedrock(pos.getDimension()));
        builder.putString("id", String.format("%08X", packet.getTrackingId()));

        builder.putByte("version", (byte) 1); // Not sure what this is for
        builder.putByte("status", (byte) 0); // Not sure what this is for

        // Build the position for the update
        IntList posList = new IntArrayList();
        posList.add(pos.getX());
        posList.add(pos.getY());
        posList.add(pos.getZ());
        builder.putList("pos", NbtType.INT, posList);
        broadcastPacket.setTag(builder.build());

        session.sendUpstreamPacket(broadcastPacket);
    }
}
