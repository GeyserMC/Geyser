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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.steveice10.mc.protocol.data.game.world.map.MapData;
import com.github.steveice10.mc.protocol.data.game.world.map.MapIcon;
import com.github.steveice10.mc.protocol.data.game.world.map.MapIconType;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMapDataPacket;
import com.nukkitx.protocol.bedrock.data.MapDecoration;
import com.nukkitx.protocol.bedrock.data.MapTrackedObject;
import com.nukkitx.protocol.bedrock.packet.ClientboundMapItemDataPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
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

        try {
            GeyserConnector.getInstance().getLogger().debug(new ObjectMapper().writeValueAsString(packet.getIcons()));
        } catch (Exception e) { }

        // Bedrock needs a tracked entity id
        int id = 0;
        for (MapIcon icon : packet.getIcons()) {
            BedrockMapIcon bedrockMapIcon = new BedrockMapIcon(icon.getIconType());

            mapItemDataPacket.getTrackedObjects().add(new MapTrackedObject(id));
            mapItemDataPacket.getDecorations().add(new MapDecoration(bedrockMapIcon.getImageID(), icon.getIconRotation(), icon.getCenterX(), icon.getCenterZ(), "", bedrockMapIcon.getImageColour()));
            id++;
        }

        session.getUpstream().getSession().sendPacket(mapItemDataPacket);
    }

    @Getter
    @AllArgsConstructor
    class BedrockMapIcon {
        private int imageID;
        private int imageColour;

        public BedrockMapIcon(MapIconType type) {
            imageID = 0;
            imageColour = -1;

            switch (type) {
                case WHITE_ARROW:
                    imageID = 0;
                    break;
                case GREEN_ARROW: // For item frames
                    imageID = 7; // Displays as an item frame normal green markers are 1
                    break;
                case RED_ARROW:
                    imageID = 2;
                    break;
                case BLUE_ARROW:
                    imageID = 3;
                    break;
                case TREASURE_MARKER:
                    imageID = 4;
                    break;
                case RED_POINTER:
                    imageID = 5;
                    break;
                case WHITE_CIRCLE:
                    imageID = 6;
                    break;
                case SMALL_WHITE_CIRCLE:
                    imageID = 13;
                    break;
                case MANSION:
                    imageID = 14;
                    break;
                case TEMPLE:
                    imageID = 15;
                    break;
                case WHITE_BANNER:
                    imageColour = toARGB(255, 255, 255);
                    imageID = 13;
                    break;
                case ORANGE_BANNER:
                    imageColour = toARGB(249, 128, 29);
                    imageID = 13;
                    break;
                case MAGENTA_BANNER:
                    imageColour = toARGB(199, 78, 189);
                    imageID = 13;
                    break;
                case LIGHT_BLUE_BANNER:
                    imageColour = toARGB(58, 179, 218);
                    imageID = 13;
                    break;
                case YELLOW_BANNER:
                    imageColour = toARGB(254, 216, 61);
                    imageID = 13;
                    break;
                case LIME_BANNER:
                    imageColour = toARGB(128, 199, 31);
                    imageID = 13;
                    break;
                case PINK_BANNER:
                    imageColour = toARGB(243, 139, 170);
                    imageID = 13;
                    break;
                case GRAY_BANNER:
                    imageColour = toARGB(71, 79, 82);
                    imageID = 13;
                    break;
                case LIGHT_GRAY_BANNER:
                    imageColour = toARGB(157, 157, 151);
                    imageID = 13;
                    break;
                case CYAN_BANNER:
                    imageColour = toARGB(22, 156, 156);
                    imageID = 13;
                    break;
                case PURPLE_BANNER:
                    imageColour = toARGB(137, 50, 184);
                    imageID = 13;
                    break;
                case BLUE_BANNER:
                    imageColour = toARGB(60, 68, 170);
                    imageID = 13;
                    break;
                case BROWN_BANNER:
                    imageColour = toARGB(131, 84, 50);
                    imageID = 13;
                    break;
                case GREEN_BANNER:
                    imageColour = toARGB(94, 124, 22);
                    imageID = 13;
                    break;
                case RED_BANNER:
                    imageColour = toARGB(176, 46, 38);
                    imageID = 13;
                    break;
                case BLACK_BANNER:
                    imageColour = toARGB(29, 29, 33);
                    imageID = 13;
                    break;
            }
        }

        public int toARGB(int red, int green, int blue) {
            int alpha = 255;

            return ((alpha & 0xFF) << 24) |
                    ((red & 0xFF) << 16) |
                    ((green & 0xFF) << 8) |
                    ((blue & 0xFF) << 0);
        }
    }
}
