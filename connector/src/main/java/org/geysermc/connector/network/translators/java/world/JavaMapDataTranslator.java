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
import com.github.steveice10.mc.protocol.data.game.world.map.MapIcon;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMapDataPacket;
import com.nukkitx.protocol.bedrock.data.MapDecoration;
import com.nukkitx.protocol.bedrock.data.MapTrackedObject;
import com.nukkitx.protocol.bedrock.packet.ClientboundMapItemDataPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.BedrockMapIcon;
import org.geysermc.connector.utils.MapColor;

@Translator(packet = ServerMapDataPacket.class)
public class JavaMapDataTranslator extends PacketTranslator<ServerMapDataPacket> {
    @Override
    public void translate(ServerMapDataPacket packet, GeyserSession session) {
        ClientboundMapItemDataPacket mapItemDataPacket = new ClientboundMapItemDataPacket();
        boolean shouldStore = false;

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

            // We have a full map image, this usually only happens on spawn for the initial image
            if (mapItemDataPacket.getWidth() == 128 && mapItemDataPacket.getHeight() == 128) {
                shouldStore = true;
            }

            // Every int entry is an ABGR color
            int[] colors = new int[data.getData().length];

            int idx = 0;
            for (byte colorId : data.getData()) {
                colors[idx++] = MapColor.fromId(colorId & 0xFF).toABGR();
            }

            mapItemDataPacket.setColors(colors);
        }

        // Bedrock needs an entity id to display an icon
        int id = 0;
        for (MapIcon icon : packet.getIcons()) {
            BedrockMapIcon bedrockMapIcon = BedrockMapIcon.fromType(icon.getIconType());

            mapItemDataPacket.getTrackedObjects().add(new MapTrackedObject(id));
            mapItemDataPacket.getDecorations().add(new MapDecoration(bedrockMapIcon.getIconID(), icon.getIconRotation(), icon.getCenterX(), icon.getCenterZ(), "", bedrockMapIcon.toARGB()));
            id++;
        }

        // Store the map to send when the client requests it, as bedrock expects the data after a MapInfoRequestPacket
        if (shouldStore) {
            session.getStoredMaps().put(mapItemDataPacket.getUniqueMapId(), mapItemDataPacket);
        }

        // Send anyway just in case
        session.sendUpstreamPacket(mapItemDataPacket);
    }
}
