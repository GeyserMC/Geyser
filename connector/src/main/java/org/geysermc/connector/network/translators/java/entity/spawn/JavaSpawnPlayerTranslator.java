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

package org.geysermc.connector.network.translators.java.entity.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.nukkitx.math.vector.Vector3f;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.SkinUtils;

@Translator(packet = ServerSpawnPlayerPacket.class)
public class JavaSpawnPlayerTranslator extends PacketTranslator<ServerSpawnPlayerPacket> {

    @Override
    public void translate(ServerSpawnPlayerPacket packet, GeyserSession session) {
        Vector3f position = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
        Vector3f rotation = Vector3f.from(packet.getYaw(), packet.getPitch(), packet.getYaw());

        PlayerEntity entity = session.getEntityCache().getPlayerEntity(packet.getUuid());
        if (entity == null) {
            GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.entity.player.failed_list", packet.getUuid()));
            return;
        }

        entity.setEntityId(packet.getEntityId());
        entity.setPosition(position);
        entity.setRotation(rotation);
        session.getEntityCache().cacheEntity(entity);

        if (session.getUpstream().isInitialized()) {
            entity.sendPlayer(session);
            SkinUtils.requestAndHandleSkinAndCape(entity, session, null);
        }
    }
}
