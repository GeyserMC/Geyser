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

package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.AddItemEntityPacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemTranslator;

public class ItemEntity extends Entity {

    protected ItemData item;

    public ItemEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position.add(0d, entityType.getOffset(), 0d), motion, rotation);
    }

    @Override
    public void setMotion(Vector3f motion) {
        if (isOnGround())
            motion = Vector3f.from(motion.getX(), 0, motion.getZ());

        super.setMotion(motion);
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        super.moveAbsolute(session, position.add(0d, this.entityType.getOffset(), 0d), rotation, isOnGround, teleported);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        if (item == null) {
            return;
        }
        valid = true;
        AddItemEntityPacket itemPacket = new AddItemEntityPacket();
        itemPacket.setRuntimeEntityId(geyserId);
        itemPacket.setPosition(position.add(0d, this.entityType.getOffset(), 0d));
        itemPacket.setMotion(motion);
        itemPacket.setUniqueEntityId(geyserId);
        itemPacket.setFromFishing(false);
        itemPacket.setItemInHand(item);
        session.sendUpstreamPacket(itemPacket);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 8) {
            item = ItemTranslator.translateToBedrock(session, (ItemStack) entityMetadata.getValue());
            despawnEntity(session);
            spawnEntity(session);
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }
}
