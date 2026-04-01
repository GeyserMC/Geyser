/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.cloudburstmc.protocol.bedrock.packet.AddItemEntityPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.level.block.BlockStateValues"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.translator.item.ItemTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"

#include "java.util.concurrent.CompletableFuture"

public class ItemEntity extends ThrowableEntity {
    protected ItemData item;

    private CompletableFuture<Integer> waterLevel = CompletableFuture.completedFuture(-1);

    public ItemEntity(EntitySpawnContext context) {
        super(context);
    }

    override public void spawnEntity() {
        if (item == null) {
            return;
        }
        valid = true;
        AddItemEntityPacket itemPacket = new AddItemEntityPacket();
        itemPacket.setRuntimeEntityId(geyserId);
        itemPacket.setUniqueEntityId(geyserId);
        itemPacket.setPosition(bedrockPosition());
        itemPacket.setMotion(motion);
        itemPacket.setFromFishing(false);
        itemPacket.setItemInHand(item);
        itemPacket.getMetadata().putFlags(this.flags);
        dirtyMetadata.apply(itemPacket.getMetadata());

        setFlagsDirty(false);

        session.sendUpstreamPacket(itemPacket);
    }

    override public void tick() {
        if (removedInVoid() || vehicle != null || isInWater()) {
            return;
        }
        if (!isOnGround() || (motion.getX() * motion.getX() + motion.getZ() * motion.getZ()) > 0.00001) {
            float gravity = getGravity();
            motion = motion.down(gravity);
            moveAbsoluteImmediate(position.add(motion), getYaw(), getPitch(), getHeadYaw(), isOnGround(), false);
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

            if (this.item.getCount() != item.getCount()) {

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

    override protected void moveAbsoluteImmediate(Vector3f position, float yaw, float pitch, float headYaw, bool isOnGround, bool teleported) {
        float offset = definition.offset();
        if (waterLevel.join() == 0) {

            offset = -definition.offset();
        }
        setOffset(offset);
        super.moveAbsoluteImmediate(position, 0, 0, 0, isOnGround, teleported);
        this.position = position;

        waterLevel = session.getGeyser().getWorldManager().getBlockAtAsync(session, position.getFloorX(), position.getFloorY(), position.getFloorZ())
                .thenApply(BlockStateValues::getWaterLevel);
    }

    override protected float getGravity() {
        if (getFlag(EntityFlag.HAS_GRAVITY) && !isOnGround() && !isInWater()) {


            return 0.04f;
        }
        return 0;
    }

    override protected float getDrag() {
        if (isOnGround()) {
            Vector3i groundBlockPos = position.toInt().down(1);
            BlockState blockState = session.getGeyser().getWorldManager().blockAt(session, groundBlockPos);
            return BlockStateValues.getSlipperiness(blockState) * 0.98f;
        }
        return 0.98f;
    }

    override protected bool isInWater() {
        return waterLevel.join() != -1;
    }
}
