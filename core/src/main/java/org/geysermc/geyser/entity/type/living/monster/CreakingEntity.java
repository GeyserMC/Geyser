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

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventGenericPacket;
import org.geysermc.geyser.entity.properties.type.EnumProperty;
import org.geysermc.geyser.entity.properties.type.IntProperty;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.impl.IdentifierImpl;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;

import java.util.Optional;

public class CreakingEntity extends MonsterEntity {

    public static final EnumProperty<CreakingState> STATE_PROPERTY = new EnumProperty<>(
        IdentifierImpl.of("creaking_state"),
        CreakingState.class,
        CreakingState.NEUTRAL
    );

    // also, the creaking seems to have this minecraft:creaking_swaying_ticks thingy
    // which i guess is responsible for some animation?
    // it's sent over the network, all 6 "stages" 50ms in between of each other.
    // no clue what it's used for tbh, so i'm not gonna bother implementing it
    // - chris
    // update: this still holds true, even a refactor later :(
    public static final IntProperty SWAYING_TICKS_PROPERTY = new IntProperty(
        IdentifierImpl.of("creaking_swaying_ticks"),
        6, 0, 0
    );

    private Vector3i homePosition;

    public CreakingEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        setFlag(EntityFlag.FIRE_IMMUNE, true);
    }

    public void setCanMove(EntityMetadata<Boolean,? extends MetadataType<Boolean>> booleanEntityMetadata) {
        setFlag(EntityFlag.BODY_ROTATION_BLOCKED, !booleanEntityMetadata.getValue());
        STATE_PROPERTY.apply(propertyManager, booleanEntityMetadata.getValue() ? CreakingState.HOSTILE_UNOBSERVED : CreakingState.HOSTILE_OBSERVED);
        updateBedrockEntityProperties();
    }

    public void setActive(EntityMetadata<Boolean,? extends MetadataType<Boolean>> booleanEntityMetadata) {
        if (!booleanEntityMetadata.getValue()) {
            STATE_PROPERTY.apply(propertyManager, CreakingState.NEUTRAL);
        }
    }

    public void setIsTearingDown(EntityMetadata<Boolean,? extends MetadataType<Boolean>> booleanEntityMetadata) {
        if (booleanEntityMetadata.getValue()) {
            STATE_PROPERTY.apply(propertyManager, CreakingState.CRUMBLING);
            updateBedrockEntityProperties();
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
                    .putInt("CreakingAmount", 20)
                    .putFloat("CreakingX", position.getX())
                    .putFloat("CreakingY", position.getY())
                    .putFloat("CreakingZ", position.getZ())
                    .putInt("HeartAmount", 20)
                    .putFloat("HeartX", homePosition.getX())
                    .putFloat("HeartY", homePosition.getY())
                    .putFloat("HeartZ", homePosition.getZ())
                    .build()
            );

            session.sendUpstreamPacket(levelEventGenericPacket);
        }
    }

    public enum CreakingState {
        NEUTRAL,
        HOSTILE_OBSERVED,
        HOSTILE_UNOBSERVED,
        TWITCHING,
        CRUMBLING
    }
}
