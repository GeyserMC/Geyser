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

package org.geysermc.geyser.session.cache;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.BossEventPacket;
import com.nukkitx.protocol.bedrock.packet.RemoveEntityPacket;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;

@AllArgsConstructor
public class BossBar {
    private final GeyserSession session;

    private final long entityId;
    private Component title;
    private float health;
    private int color;
    private final int overlay;
    private final int darkenSky;

    public void addBossBar() {
        addBossEntity();
        updateBossBar();
    }

    //TODO: There is a player unique entity ID - if this didn't exist before, we may be able to get rid of our hack

    public void updateBossBar() {
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.CREATE);
        bossEventPacket.setTitle(MessageTranslator.convertMessage(title, session.getLocale()));
        bossEventPacket.setHealthPercentage(health);
        bossEventPacket.setColor(color); //ignored by client
        bossEventPacket.setOverlay(overlay);
        bossEventPacket.setDarkenSky(darkenSky);

        session.sendUpstreamPacket(bossEventPacket);
    }

    public void updateTitle(Component title) {
        this.title = title;
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.UPDATE_NAME);
        bossEventPacket.setTitle(MessageTranslator.convertMessage(title, session.getLocale()));

        session.sendUpstreamPacket(bossEventPacket);
    }

    public void updateHealth(float health) {
        this.health = health;
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.UPDATE_PERCENTAGE);
        bossEventPacket.setHealthPercentage(health);

        session.sendUpstreamPacket(bossEventPacket);
    }

    public void updateColor(int color) {
        this.color = color;
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.UPDATE_STYLE);
        bossEventPacket.setColor(color);

        session.sendUpstreamPacket(bossEventPacket);
    }

    public void removeBossBar() {
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.REMOVE);

        session.sendUpstreamPacket(bossEventPacket);
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
        addEntityPacket.setPosition(session.getPlayerEntity().getPosition().sub(0D, -10D, 0D));
        addEntityPacket.setRotation(Vector3f.ZERO);
        addEntityPacket.setMotion(Vector3f.ZERO);
        addEntityPacket.getMetadata()
                .putFloat(EntityData.SCALE, 0F)
                .putFloat(EntityData.BOUNDING_BOX_WIDTH, 0F)
                .putFloat(EntityData.BOUNDING_BOX_HEIGHT, 0F);

        session.sendUpstreamPacket(addEntityPacket);
    }

    private void removeBossEntity() {
        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(entityId);

        session.sendUpstreamPacket(removeEntityPacket);
    }
}
