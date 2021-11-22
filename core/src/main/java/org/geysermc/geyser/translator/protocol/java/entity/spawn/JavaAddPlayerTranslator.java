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

package org.geysermc.geyser.translator.protocol.java.entity.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.nukkitx.math.vector.Vector3f;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.skin.SkinManager;

@Translator(packet = ClientboundAddPlayerPacket.class)
public class JavaAddPlayerTranslator extends PacketTranslator<ClientboundAddPlayerPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundAddPlayerPacket packet) {
        Vector3f position = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
        float yaw = packet.getYaw();
        float pitch = packet.getPitch();
        float headYaw = packet.getYaw();

        PlayerEntity entity;
        if (packet.getUuid().equals(session.getPlayerEntity().getUuid())) {
            // Server is sending a fake version of the current player
            entity = new PlayerEntity(session, packet.getEntityId(), session.getEntityCache().getNextEntityId().incrementAndGet(), session.getPlayerEntity().getProfile(), position, Vector3f.ZERO, yaw, pitch, headYaw);
        } else {
            entity = session.getEntityCache().getPlayerEntity(packet.getUuid());
            if (entity == null) {
                GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.entity.player.failed_list", packet.getUuid()));
                return;
            }

            entity.setEntityId(packet.getEntityId());
            entity.setPosition(position);
            entity.setYaw(yaw);
            entity.setPitch(pitch);
            entity.setHeadYaw(headYaw);
        }
        session.getEntityCache().cacheEntity(entity);

        entity.sendPlayer();
        SkinManager.requestAndHandleSkinAndCape(entity, session, null);
    }
}
