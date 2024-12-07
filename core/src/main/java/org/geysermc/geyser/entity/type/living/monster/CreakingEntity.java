/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.living.monster;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventGenericPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;

import java.util.Optional;
import java.util.UUID;

/*
 * Relevant bits:
 * - LevelSoundEvent2Packet(sound=SPAWN, position=(233.5, 112.295, 4717.5), extraData=-1, identifier=minecraft:creaking, babySound=false, relativeVolumeDisabled=false)
 * - [11:29:34:768] [CLIENT BOUND] - LevelSoundEvent2Packet(sound=CREAKING_HEART_SPAWN, position=(233.0, 110.0, 4717.0), extraData=-1, identifier=minecraft:creaking, babySound=false, relativeVolumeDisabled=false)
 * - [11:29:34:768] [CLIENT BOUND] - LevelSoundEvent2Packet(sound=CREAKING_HEART_SPAWN, position=(235.0, 113.0, 4722.0), extraData=13734, identifier=, babySound=false, relativeVolumeDisabled=false)
 * - [11:29:34:768] [CLIENT BOUND] - LevelEventPacket(type=PARTICLE_MOB_BLOCK_SPAWN, position=(233.0, 110.0, 4717.0), data=769)
 *
 */
public class CreakingEntity extends MonsterEntity {

    private Vector3i homePosition;

    public static final String CREAKING_STATE = "minecraft:creaking_state";
    public static final String CREAKING_SWAYING_TICKS = "minecraft:creaking_swaying_ticks";

    public CreakingEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        setFlag(EntityFlag.HIDDEN_WHEN_INVISIBLE, true);
        setFlag(EntityFlag.FIRE_IMMUNE, true);
    }

    @Override
    public void addAdditionalSpawnData(AddEntityPacket addEntityPacket) {
        propertyManager.add(CREAKING_STATE, "neutral");
        propertyManager.add("minecraft:creaking_swaying_ticks", 0);
        propertyManager.applyIntProperties(addEntityPacket.getProperties().getIntProperties());
    }

    public void setCanMove(EntityMetadata<Boolean,? extends MetadataType<Boolean>> booleanEntityMetadata) {
        if (booleanEntityMetadata.getValue()) {
            setFlag(EntityFlag.BODY_ROTATION_BLOCKED, false);

            // unfreeze sound? SoundEvent.UNFREEZE
            propertyManager.add(CREAKING_STATE, "hostile_unobserved");
            updateBedrockEntityProperties();
        } else {
            setFlag(EntityFlag.BODY_ROTATION_BLOCKED, true);
            propertyManager.add(CREAKING_STATE, "hostile_observed");
            updateBedrockEntityProperties();
        }

        GeyserImpl.getInstance().getLogger().warning("set can move; " + booleanEntityMetadata.toString());
    }

    public void setActive(EntityMetadata<Boolean,? extends MetadataType<Boolean>> booleanEntityMetadata) {
        if (booleanEntityMetadata.getValue()) {
//            LevelSoundEvent2Packet addEntityPacket = new LevelSoundEvent2Packet();
//            addEntityPacket.setIdentifier("minecraft:creaking");
//            addEntityPacket.setPosition(position);
//            addEntityPacket.setBabySound(false);
//            addEntityPacket.setSound(SoundEvent.ACTIVATE);
//            addEntityPacket.setExtraData(-1);
//            session.sendUpstreamPacket(addEntityPacket);

//            setFlag(EntityFlag.HIDDEN_WHEN_INVISIBLE, true);
//            setFlag(EntityFlag.BODY_ROTATION_BLOCKED, true);
        } else {
            propertyManager.add(CREAKING_STATE, "neutral");
        }
        GeyserImpl.getInstance().getLogger().warning("set active; " + booleanEntityMetadata.toString());
    }

    public void setIsTearingDown(EntityMetadata<Boolean,? extends MetadataType<Boolean>> booleanEntityMetadata) {
        GeyserImpl.getInstance().getLogger().warning("set isTearingDown; " + booleanEntityMetadata.toString());
        if (booleanEntityMetadata.getValue()) {
            propertyManager.add(CREAKING_STATE, "crumbling");
            updateBedrockEntityProperties();
//            LevelEventPacket levelEventPacket = new LevelEventPacket();
//            levelEventPacket.setType(ParticleType.CREAKING_CRUMBLE);
//            levelEventPacket.setPosition(position);
//            levelEventPacket.setData(0);
        }
    }

    public void setHomePos(EntityMetadata<Optional<Vector3i>,? extends MetadataType<Optional<Vector3i>>> optionalEntityMetadata) {
        if (optionalEntityMetadata.getValue().isPresent()) {
            this.homePosition = optionalEntityMetadata.getValue().get();
        } else {
            this.homePosition = null;
        }
    }

    public void createParticleBeam() {
        if (this.homePosition != null) {
            LevelEventGenericPacket levelEventGenericPacket = new LevelEventGenericPacket();
            levelEventGenericPacket.setType(LevelEvent.PARTICLE_CREAKING_HEART_TRIAL);
            levelEventGenericPacket.setTag(
                NbtMap.builder()
                    .putInt("CreakingAmount", 0)
                    .putFloat("CreakingX", position.getX())
                    .putFloat("CreakingY", position.getY())
                    .putFloat("CreakingZ", position.getZ())
                    .putInt("HeartAmount", 20)
                    .putFloat("HeartX", homePosition.getX())
                    .putFloat("HeartY", homePosition.getY())
                    .putFloat("HeartZ", homePosition.getZ())
                    .build()
            );

            GeyserImpl.getInstance().getLogger().warning(levelEventGenericPacket.toString());
            session.sendUpstreamPacket(levelEventGenericPacket);
        }
    }
}
