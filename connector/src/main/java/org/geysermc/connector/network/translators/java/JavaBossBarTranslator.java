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

package org.geysermc.connector.network.translators.java;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerBossBarPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.BossEventPacket;
import com.nukkitx.protocol.bedrock.packet.RemoveEntityPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.MessageUtils;

public class JavaBossBarTranslator extends PacketTranslator<ServerBossBarPacket> {
    @Override
    public void translate(ServerBossBarPacket packet, GeyserSession session) {
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(session.getEntityCache().getBossBar(packet.getUuid()));

        switch (packet.getAction()) {
            case ADD:
                long entityId = session.getEntityCache().addBossBar(packet.getUuid());
                addBossEntity(session, entityId);

                bossEventPacket.setAction(BossEventPacket.Action.SHOW);
                bossEventPacket.setBossUniqueEntityId(entityId);
                bossEventPacket.setTitle(MessageUtils.getBedrockMessage(packet.getTitle()));
                bossEventPacket.setHealthPercentage(packet.getHealth());
                bossEventPacket.setColor(0); //ignored by client
                bossEventPacket.setOverlay(1);
                bossEventPacket.setDarkenSky(0);
                break;
            case UPDATE_TITLE:
                bossEventPacket.setAction(BossEventPacket.Action.TITLE);
                bossEventPacket.setTitle(MessageUtils.getBedrockMessage(packet.getTitle()));
                break;
            case UPDATE_HEALTH:
                bossEventPacket.setAction(BossEventPacket.Action.HEALTH_PERCENTAGE);
                bossEventPacket.setHealthPercentage(packet.getHealth());
                break;
            case REMOVE:
                bossEventPacket.setAction(BossEventPacket.Action.HIDE);
                removeBossEntity(session, session.getEntityCache().removeBossBar(packet.getUuid()));
                break;
            case UPDATE_STYLE:
            case UPDATE_FLAGS:
                //todo
                return;
        }

        session.getUpstream().sendPacket(bossEventPacket);
    }

    /**
     * Bedrock still needs an entity to display the BossBar.<br>
     * Just like 1.8 but it doesn't care about which entity
     */
    private void addBossEntity(GeyserSession session, long entityId) {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setUniqueEntityId(entityId);
        addEntityPacket.setRuntimeEntityId(entityId);
        addEntityPacket.setIdentifier("minecraft:creeper");
        addEntityPacket.setEntityType(33);
        addEntityPacket.setPosition(session.getPlayerEntity().getPosition());
        addEntityPacket.setRotation(Vector3f.ZERO);
        addEntityPacket.setMotion(Vector3f.ZERO);
        addEntityPacket.getMetadata().put(EntityData.SCALE, 0.01F); // scale = 0 doesn't work?

        session.getUpstream().sendPacket(addEntityPacket);
    }

    private void removeBossEntity(GeyserSession session, long entityId) {
        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(entityId);

        session.getUpstream().sendPacket(removeEntityPacket);
    }
}
