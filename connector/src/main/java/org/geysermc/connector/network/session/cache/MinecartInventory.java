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
import org.geysermc.connector.utils.LocaleUtils;
import org.geysermc.connector.utils.MessageUtils;
import lombok.Getter;

@AllArgsConstructor
public class MinecartInventory {

    private GeyserSession session;

    @Getter
    private long entityId;
    private String title;

    public void addMinecartInventory() {
        addMinecartEntity();
        // updateMinecartInventory();
    }

    /* public void updateMinecartInventory() {
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.SHOW);
        bossEventPacket.setTitle(MessageUtils.getTranslatedBedrockMessage(title, session.getClientData().getLanguageCode()));
        bossEventPacket.setHealthPercentage(health);
        bossEventPacket.setColor(color); //ignored by client
        bossEventPacket.setOverlay(overlay);
        bossEventPacket.setDarkenSky(darkenSky);

        session.sendUpstreamPacket(bossEventPacket);
    } */

    public void updateTitle(String title) {
        this.title = title;
        /* BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.TITLE);
        bossEventPacket.setTitle(MessageUtils.getTranslatedBedrockMessage(title, session.getClientData().getLanguageCode()));

        session.sendUpstreamPacket(bossEventPacket); */
    }

    public void removeMinecartInventory() {
        /* BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.HIDE);

        session.sendUpstreamPacket(bossEventPacket); */
        removeMinecartEntity();
    }

    /**
     * Bedrock still needs an entity to display the MinecartInventory.<br>
     * Just like 1.8 but it doesn't care about which entity
     */
    private void addMinecartEntity() {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setUniqueEntityId(entityId);
        addEntityPacket.setRuntimeEntityId(entityId);
        // You can't hide the chest of a chest_minecart but Bedrock accepts this too
        addEntityPacket.setIdentifier("minecraft:minecart");
        addEntityPacket.setEntityType(0);
        addEntityPacket.setPosition(session.getPlayerEntity().getPosition().sub(0D, 3D, 0D));
        addEntityPacket.setRotation(Vector3f.ZERO);
        addEntityPacket.setMotion(Vector3f.ZERO);
        addEntityPacket.getMetadata()
                .putFloat(EntityData.SCALE, 0F)
                .putFloat(EntityData.BOUNDING_BOX_WIDTH, 0F)
                .putFloat(EntityData.BOUNDING_BOX_HEIGHT, 0F);

        addEntityPacket.getMetadata().put(EntityData.CONTAINER_TYPE, 10);
        addEntityPacket.getMetadata().put(EntityData.CONTAINER_BASE_SIZE, 27);
        addEntityPacket.getMetadata().put(EntityData.CONTAINER_EXTRA_SLOTS_PER_STRENGTH, 0);
        addEntityPacket.getMetadata().put(EntityData.NAMETAG, title);

        session.sendUpstreamPacket(addEntityPacket);
    }

    private void removeMinecartEntity() {
        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(entityId);

        session.sendUpstreamPacket(removeEntityPacket);
    }
}
