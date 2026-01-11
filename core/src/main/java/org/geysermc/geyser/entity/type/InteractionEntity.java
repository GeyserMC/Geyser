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

import net.kyori.adventure.text.Component;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.living.ArmorStandEntity;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;

import java.util.Optional;

public class InteractionEntity extends Entity {

    /**
     * true - java client hears swing sound when attacking, and arm swings when right-clicking
     * false - java client hears no swing sound when attacking, and arm does not swing when right-clicking
     */
    private boolean response = false;

    public InteractionEntity(EntitySpawnContext context) {
        super(context);
    }

    /**
     * On Java Edition, interaction entities can have a nametag. Invisibility hides the name on Bedrock.
     * By having a second entity, we can still show the nametag while keeping the interaction entity invisible.
     */
    private ArmorStandEntity secondEntity = null;

    private boolean isNameTagVisible = false;

    private boolean isVisible = true;

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();

        // hide the armor stand but keep the hitbox active
        setFlag(EntityFlag.INVISIBLE, true);
    }

    @Override
    protected void setInvisible(boolean value) {
        // Always invisible; would reveal the armor stand otherwise
        isVisible = value;
        this.updateNameTag();
    }

    @Override
    public void setDisplayNameVisible(BooleanEntityMetadata entityMetadata) {
        isNameTagVisible = entityMetadata.getPrimitiveValue();
        this.updateNameTag();
    }

    @Override
    public InteractionResult interact(Hand hand) {
        // these InteractionResults do mirror the java client
        // but the bedrock client won't arm swing itself because of our armor stand workaround
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

    @Override
    public void despawnEntity() {
        if (secondEntity != null) {
            secondEntity.despawnEntity();
        }
        super.despawnEntity();
    }

    @Override
    public void setDisplayName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        super.setDisplayName(entityMetadata);
        this.updateNameTag();
    }

    @Override
    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        moveAbsoluteRaw(position.add(relX, relY, relZ), yaw, pitch, headYaw, isOnGround, false);
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        if (secondEntity != null) {
            secondEntity.moveAbsoluteRaw(position.up(getBoundingBoxHeight()), yaw, pitch, headYaw, isOnGround, teleported);
        }
        super.moveAbsoluteRaw(position, yaw, pitch, headYaw, isOnGround, teleported);
    }

    public void setWidth(FloatEntityMetadata width) {
        setBoundingBoxWidth(width.getPrimitiveValue());
    }

    public void setHeight(FloatEntityMetadata height) {
        // Bedrock does *not* like high values being placed here
        // https://gist.github.com/Owen1212055/f5d59169d3a6a5c32f0c173d57eb199d recommend(s/ed) using the tactic
        // https://github.com/GeyserMC/Geyser/issues/4688
        setBoundingBoxHeight(Math.min(height.getPrimitiveValue(), 64f));

        if (secondEntity != null) {
            secondEntity.moveAbsoluteRaw(position.up(getBoundingBoxHeight()), yaw, pitch, headYaw, onGround, true);
        }
    }

    public void setResponse(BooleanEntityMetadata response) {
        this.response = response.getPrimitiveValue();
    }

    public void updateNameTag() {
        if (this.nametag.isBlank() || !isVisible) {
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
        secondEntity.getDirtyMetadata().put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, isNameTagVisible ? (byte) 1 : (byte) 0);
        // Scale to 0 to show nametag
        secondEntity.setScale(0f);
        // No bounding box as we don't want to interact with this entity
        secondEntity.getDirtyMetadata().put(EntityDataTypes.WIDTH, 0.0f);
        secondEntity.getDirtyMetadata().put(EntityDataTypes.HEIGHT, 0.0f);
        secondEntity.getDirtyMetadata().put(EntityDataTypes.HITBOX, NbtMap.EMPTY);
        if (!secondEntity.valid) { // Spawn the entity once
            secondEntity.spawnEntity();
        }
    }
}
