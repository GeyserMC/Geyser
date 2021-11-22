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

package org.geysermc.geyser.translator.protocol.java.level;

import com.github.steveice10.mc.protocol.data.game.level.map.MapData;
import com.github.steveice10.mc.protocol.data.game.level.map.MapIcon;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundMapItemDataPacket;
import com.nukkitx.protocol.bedrock.data.MapDecoration;
import com.nukkitx.protocol.bedrock.data.MapTrackedObject;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.level.BedrockMapIcon;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.geyser.level.MapColor;

@Translator(packet = ClientboundMapItemDataPacket.class)
public class JavaMapItemDataTranslator extends PacketTranslator<ClientboundMapItemDataPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundMapItemDataPacket packet) {
        com.nukkitx.protocol.bedrock.packet.ClientboundMapItemDataPacket mapItemDataPacket = new com.nukkitx.protocol.bedrock.packet.ClientboundMapItemDataPacket();
        boolean shouldStore = false;

        mapItemDataPacket.setUniqueMapId(packet.getMapId());
        mapItemDataPacket.setDimensionId(DimensionUtils.javaToBedrock(session.getDimension()));
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

            // Every int entry is an ARGB color
            int[] colors = new int[data.getData().length];

            int idx = 0;
            for (byte colorId : data.getData()) {
                colors[idx++] = MapColor.fromId(colorId & 0xFF).getARGB();
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
