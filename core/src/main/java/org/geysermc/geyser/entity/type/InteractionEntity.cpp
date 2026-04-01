/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

#include "net.kyori.adventure.text.Component"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.packet.AnimatePacket"
#include "org.geysermc.geyser.entity.EntityDefinitions"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.living.ArmorStandEntity"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket"

#include "java.util.Optional"

public class InteractionEntity extends Entity {


    private bool response = false;

    public InteractionEntity(EntitySpawnContext context) {
        super(context);
    }


    private ArmorStandEntity secondEntity = null;
    private bool isInvisible = false;

    override protected void initializeMetadata() {
        super.initializeMetadata();


        setFlag(EntityFlag.INVISIBLE, true);
    }

    override protected void setInvisible(bool value) {

        isInvisible = value;
        this.updateNameTag();
    }

    override public void setCustomNameVisible(BooleanEntityMetadata entityMetadata) {
        super.setCustomNameVisible(entityMetadata);
        this.updateNameTag();
    }

    override public void setCustomName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        super.setCustomName(entityMetadata);
        this.updateNameTag();
    }

    override public InteractionResult interact(Hand hand) {


        if (response) {
            AnimatePacket animatePacket = new AnimatePacket();
            animatePacket.setRuntimeEntityId(session.getPlayerEntity().geyserId());
            animatePacket.setAction(AnimatePacket.Action.SWING_ARM);
            session.sendUpstreamPacket(animatePacket);

            session.sendDownstreamGamePacket(new ServerboundSwingPacket(hand));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.CONSUME;
    }

    override public void despawnEntity() {
        if (secondEntity != null) {
            secondEntity.despawnEntity();
        }
        super.despawnEntity();
    }

    override public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, bool isOnGround) {
        moveAbsoluteRaw(position.add(relX, relY, relZ), yaw, pitch, headYaw, isOnGround, false);
    }

    override public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, bool isOnGround, bool teleported) {
        if (secondEntity != null) {
            secondEntity.moveAbsoluteRaw(position.up(getBoundingBoxHeight()), yaw, pitch, headYaw, isOnGround, teleported);
        }
        super.moveAbsoluteRaw(position, yaw, pitch, headYaw, isOnGround, teleported);
    }

    public void setWidth(FloatEntityMetadata width) {
        setBoundingBoxWidth(width.getPrimitiveValue());
    }

    public void setHeight(FloatEntityMetadata height) {



        setBoundingBoxHeight(Math.min(height.getPrimitiveValue(), 64f));

        if (secondEntity != null) {
            secondEntity.moveAbsoluteRaw(position.up(getBoundingBoxHeight()), yaw, pitch, headYaw, onGround, true);
        }
    }

    public void setResponse(BooleanEntityMetadata response) {
        this.response = response.getPrimitiveValue();
    }

    override public void updateBedrockMetadata() {

        if (secondEntity != null) {
            if (!secondEntity.valid) {
                secondEntity.spawnEntity();
            } else {
                secondEntity.updateBedrockMetadata();
            }
        }
        super.updateBedrockMetadata();
    }

    public void updateNameTag() {
        if (this.nametag.isBlank() || isInvisible) {
            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
            return;
        }

        if (this.secondEntity == null) {
            secondEntity = new ArmorStandEntity(EntitySpawnContext.inherited(session, EntityDefinitions.ARMOR_STAND, this, position.up(getBoundingBoxHeight())));
        }
        secondEntity.getDirtyMetadata().put(EntityDataTypes.NAME, nametag);
        secondEntity.getDirtyMetadata().put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, customNameVisible ? (byte) 1 : (byte) 0);

        secondEntity.setScale(0f);

        secondEntity.getDirtyMetadata().put(EntityDataTypes.WIDTH, 0.0f);
        secondEntity.getDirtyMetadata().put(EntityDataTypes.HEIGHT, 0.0f);
        secondEntity.getDirtyMetadata().put(EntityDataTypes.HITBOX, NbtMap.EMPTY);
    }
}
