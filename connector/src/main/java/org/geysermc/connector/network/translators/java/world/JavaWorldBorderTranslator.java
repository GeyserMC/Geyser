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

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.world.WorldBorderAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerWorldBorderPacket;
import com.nukkitx.math.vector.Vector2f;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.world.WorldBorder;

@Translator(packet = ServerWorldBorderPacket.class)
public class JavaWorldBorderTranslator extends PacketTranslator<ServerWorldBorderPacket> {

    @Override
    public void translate(ServerWorldBorderPacket packet, GeyserSession session) {
        WorldBorder worldBorder = session.getWorldBorder();

        if(packet.getAction() != WorldBorderAction.INITIALIZE && worldBorder == null) {
            if (session.getWorldBorder().getWorldBorderTask() != null) {
                session.getWorldBorder().getWorldBorderTask().cancel(false);
            }
            return;
        }

        switch(packet.getAction()) {
            case INITIALIZE:
                // should be getCenterZ()
                worldBorder = new WorldBorder(Vector2f.from(packet.getCenterX(), packet.getCenterY()), packet.getOldRadius(), packet.getNewRadius(),
                        packet.getSpeed(), packet.getWarningTime(), packet.getWarningTime());

                session.setWorldBorder(worldBorder);
                break;
            case SET_SIZE:
                worldBorder.setOldRadius(packet.getRadius());
                worldBorder.setNewRadius(packet.getRadius());
                break;
            case LERP_SIZE:
                worldBorder.setOldRadius(packet.getOldRadius());
                worldBorder.setNewRadius(packet.getNewRadius());
                worldBorder.setSpeed(packet.getSpeed());
                break;
            case SET_CENTER:
                // should be getCenterZ()
                worldBorder.setCenter(Vector2f.from(packet.getCenterX(), packet.getCenterY()));
                break;
            case SET_WARNING_TIME:
                worldBorder.setWarningTime(packet.getWarningTime());
                return;
            case SET_WARNING_BLOCKS:
                worldBorder.setWarningBlocks(packet.getWarningBlocks());
                return;
        }

        worldBorder.update(session);
    }
}
