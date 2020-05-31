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
import com.nukkitx.protocol.bedrock.data.EntityLink;
import com.nukkitx.protocol.bedrock.packet.*;
import lombok.AllArgsConstructor;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LocaleUtils;
import org.geysermc.connector.utils.MessageUtils;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class MinecartInventory {

    private GeyserSession session;

    @Getter
    private long entityId;
    private String title;

    /**
     * Bedrock needs an entity or a block for the inventory
     */
    public void addMinecartInventory() {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setUniqueEntityId(entityId);
        addEntityPacket.setRuntimeEntityId(entityId);
        // You can't hide the chest of a chest_minecart but Bedrock accepts a normal minecart too
        addEntityPacket.setIdentifier("minecraft:minecart");
        addEntityPacket.setEntityType(0);
        addEntityPacket.setPosition(session.getPlayerEntity().getPosition().sub(0D, 3D, 0D));
        addEntityPacket.setRotation(Vector3f.ZERO);
        addEntityPacket.setMotion(Vector3f.ZERO);
        addEntityPacket.getMetadata()
                .putFloat(EntityData.SCALE, 0F)
                .putFloat(EntityData.BOUNDING_BOX_WIDTH, 0F)
                .putFloat(EntityData.BOUNDING_BOX_HEIGHT, 0F);

        addEntityPacket.getMetadata().put(EntityData.CONTAINER_BASE_SIZE, 27);
        addEntityPacket.getMetadata().put(EntityData.NAMETAG, title);

        session.sendUpstreamPacket(addEntityPacket);

        // Mount the minecart on the player so the inventory doesn't close when the player moves away
        SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
        EntityLink.Type type = EntityLink.Type.PASSENGER;
        linkPacket.setEntityLink(new EntityLink(session.getPlayerEntity().getGeyserId(), entityId, type, false));
        session.sendUpstreamPacket(linkPacket);

    }

    public void removeMinecartInventory() {
        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(entityId);

        session.sendUpstreamPacket(removeEntityPacket);
    }

}
