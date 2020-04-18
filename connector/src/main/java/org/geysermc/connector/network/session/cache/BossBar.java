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

package org.geysermc.connector.network.session.cache;

import com.github.steveice10.mc.protocol.data.message.Message;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.BossEventPacket;
import com.nukkitx.protocol.bedrock.packet.RemoveEntityPacket;
import lombok.AllArgsConstructor;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.MessageUtils;

@AllArgsConstructor
public class BossBar {

    private GeyserSession session;

    private long entityId;
    private Message title;
    private float health;
    private int color;
    private int overlay;
    private int darkenSky;

    public void addBossBar() {
        addBossEntity();
        updateBossBar();
    }

    public void updateBossBar() {
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.SHOW);
        bossEventPacket.setTitle(MessageUtils.getTranslatedBedrockMessage(title, session.getClientData().getLanguageCode()));
        bossEventPacket.setHealthPercentage(health);
        bossEventPacket.setColor(color); //ignored by client
        bossEventPacket.setOverlay(overlay);
        bossEventPacket.setDarkenSky(darkenSky);

        session.getUpstream().sendPacket(bossEventPacket);
    }

    public void updateTitle(Message title) {
        this.title = title;
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.TITLE);
        bossEventPacket.setTitle(MessageUtils.getTranslatedBedrockMessage(title, session.getClientData().getLanguageCode()));

        session.getUpstream().sendPacket(bossEventPacket);
    }

    public void updateHealth(float health) {
        this.health = health;
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.HEALTH_PERCENTAGE);
        bossEventPacket.setHealthPercentage(health);

        session.getUpstream().sendPacket(bossEventPacket);
    }

    public void removeBossBar() {
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.HIDE);

        session.getUpstream().sendPacket(bossEventPacket);
        removeBossEntity();
    }

    /**
     * Bedrock still needs an entity to display the BossBar.<br>
     * Just like 1.8 but it doesn't care about which entity
     */
    private void addBossEntity() {
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

    private void removeBossEntity() {
        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(entityId);

        session.getUpstream().sendPacket(removeEntityPacket);
    }
}
