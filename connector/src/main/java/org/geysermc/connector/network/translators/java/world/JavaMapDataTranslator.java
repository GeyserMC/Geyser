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

import com.github.steveice10.mc.protocol.data.game.world.map.MapData;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMapDataPacket;
import com.nukkitx.protocol.bedrock.packet.ClientboundMapItemDataPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.MapColor;

@Translator(packet = ServerMapDataPacket.class)
public class JavaMapDataTranslator extends PacketTranslator<ServerMapDataPacket> {
    @Override
    public void translate(ServerMapDataPacket packet, GeyserSession session) {
        ClientboundMapItemDataPacket mapItemDataPacket = new ClientboundMapItemDataPacket();

        mapItemDataPacket.setUniqueMapId(packet.getMapId());
        mapItemDataPacket.setDimensionId(session.getPlayerEntity().getDimension());
        mapItemDataPacket.setLocked(packet.isLocked());
        mapItemDataPacket.setScale(packet.getScale());

        MapData data = packet.getData();
        if (data != null) {
            mapItemDataPacket.setXOffset(data.getX());
            mapItemDataPacket.setYOffset(data.getY());
            mapItemDataPacket.setWidth(data.getColumns());
            mapItemDataPacket.setHeight(data.getRows());

            // Every int entry is an ABGR color
            int[] colors = new int[data.getData().length];

            int idx = 0;
            for (byte colorId : data.getData()) {
                colors[idx++] = MapColor.fromId(colorId & 0xFF).toABGR();
            }

            mapItemDataPacket.setColors(colors);
        }

        session.getUpstream().getSession().sendPacket(mapItemDataPacket);
    }
}
