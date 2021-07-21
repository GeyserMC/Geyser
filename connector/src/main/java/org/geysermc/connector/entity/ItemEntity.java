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
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.AddItemEntityPacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;

public class ItemEntity extends ThrowableEntity {

    protected ItemData item;

    public ItemEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        if (item == null) {
            return;
        }
        valid = true;
        AddItemEntityPacket itemPacket = new AddItemEntityPacket();
        itemPacket.setRuntimeEntityId(geyserId);
        itemPacket.setUniqueEntityId(geyserId);
        itemPacket.setPosition(position.add(0d, this.entityType.getOffset(), 0d));
        itemPacket.setMotion(motion);
        itemPacket.setFromFishing(false);
        itemPacket.setItemInHand(item);
        itemPacket.getMetadata().putAll(metadata);
        session.sendUpstreamPacket(itemPacket);
    }

    @Override
    public void tick(GeyserSession session) {
        if (isInWater(session)) {
            return;
        }
        if (!onGround || (motion.getX() * motion.getX() + motion.getZ() * motion.getZ()) > 0.00001) {
            float gravity = getGravity(session);
            motion = motion.down(gravity);
            moveAbsoluteImmediate(session, position.add(motion), rotation, onGround, false);
            float drag = getDrag(session);
            motion = motion.mul(drag, 0.98f, drag);
        }
    }

    @Override
    protected void moveAbsoluteImmediate(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        float offset = entityType.getOffset();
        if (inFullWater(session)) {
            // Move the item entity down so it doesn't float above the water
            offset = -entityType.getOffset();
        }
        super.moveAbsoluteImmediate(session, position.add(0, offset, 0), Vector3f.ZERO, isOnGround, teleported);
        this.position = position;
    }

    @Override
    protected float getGravity(GeyserSession session) {
        if (metadata.getFlags().getFlag(EntityFlag.HAS_GRAVITY) && !onGround && !isInWater(session)) {
            // Gravity can change if the item is in water/lava, but
            // the server calculates the motion & position for us
            return 0.04f;
        }
        return 0;
    }

    @Override
    protected float getDrag(GeyserSession session) {
        if (onGround) {
            Vector3i groundBlockPos = position.toInt().down(1);
            int blockState = session.getConnector().getWorldManager().getBlockAt(session, groundBlockPos);
            return BlockStateValues.getSlipperiness(blockState) * 0.98f;
        }
        return 0.98f;
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

    private boolean inFullWater(GeyserSession session) {
        int block = session.getConnector().getWorldManager().getBlockAt(session, position.toInt());
        return BlockStateValues.getWaterLevel(block) == 0;
    }
}
