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

package org.geysermc.geyser.entity.type;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.AddItemEntityPacket;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.level.block.BlockStateValues;

import java.util.UUID;

public class ItemEntity extends ThrowableEntity {
    protected ItemData item;

    private int waterLevel = -1;

    public ItemEntity(GeyserSession session, long entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    public void spawnEntity() {
        if (item == null) {
            return;
        }
        valid = true;
        AddItemEntityPacket itemPacket = new AddItemEntityPacket();
        itemPacket.setRuntimeEntityId(geyserId);
        itemPacket.setUniqueEntityId(geyserId);
        itemPacket.setPosition(position.add(0d, this.definition.offset(), 0d));
        itemPacket.setMotion(motion);
        itemPacket.setFromFishing(false);
        itemPacket.setItemInHand(item);
        itemPacket.getMetadata().putFlags(this.flags);
        dirtyMetadata.apply(itemPacket.getMetadata());

        setFlagsDirty(false);

        session.sendUpstreamPacket(itemPacket);
    }

    @Override
    public void tick() {
        if (isInWater()) {
            return;
        }
        if (!onGround || (motion.getX() * motion.getX() + motion.getZ() * motion.getZ()) > 0.00001) {
            float gravity = getGravity();
            motion = motion.down(gravity);
            moveAbsoluteImmediate(position.add(motion), yaw, pitch, headYaw, onGround, false);
            float drag = getDrag();
            motion = motion.mul(drag, 0.98f, drag);
        }
    }

    public void setItem(EntityMetadata<ItemStack, ?> entityMetadata) {
        ItemData item = ItemTranslator.translateToBedrock(session, entityMetadata.getValue());
        if (this.item == null) {
            this.item = item;
            spawnEntity();
        } else if (item.equals(this.item, false, true, true)) {
            // Don't bother respawning the entity if items are equal
            if (this.item.getCount() != item.getCount()) {
                // Just item count updated; let's make this easy
                this.item = item;
                EntityEventPacket packet = new EntityEventPacket();
                packet.setRuntimeEntityId(geyserId);
                packet.setType(EntityEventType.UPDATE_ITEM_STACK_SIZE);
                packet.setData(this.item.getCount());
                session.sendUpstreamPacket(packet);
            }
        } else {
            this.item = item;
            despawnEntity();
            spawnEntity();
        }
    }

    @Override
    protected void moveAbsoluteImmediate(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        float offset = definition.offset();
        if (waterLevel == 0) { // Item is in a full block of water
            // Move the item entity down so it doesn't float above the water
            offset = -definition.offset();
        }
        super.moveAbsoluteImmediate(position.add(0, offset, 0), 0, 0, 0, isOnGround, teleported);
        this.position = position;

        int block = session.getGeyser().getWorldManager().getBlockAt(session, position.toInt());
        waterLevel = BlockStateValues.getWaterLevel(block);
    }

    @Override
    protected float getGravity() {
        if (getFlag(EntityFlag.HAS_GRAVITY) && !onGround && !isInWater()) {
            // Gravity can change if the item is in water/lava, but
            // the server calculates the motion & position for us
            return 0.04f;
        }
        return 0;
    }

    @Override
    protected float getDrag() {
        if (onGround) {
            Vector3i groundBlockPos = position.toInt().down(1);
            int blockState = session.getGeyser().getWorldManager().getBlockAt(session, groundBlockPos);
            return BlockStateValues.getSlipperiness(blockState) * 0.98f;
        }
        return 0.98f;
    }

    @Override
    protected boolean isInWater() {
        return waterLevel != -1;
    }
}
