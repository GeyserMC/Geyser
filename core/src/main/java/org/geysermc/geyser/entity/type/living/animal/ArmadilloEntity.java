/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.living.animal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.ArmadilloState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ArmadilloEntity extends AnimalEntity {
    private ArmadilloState armadilloState = ArmadilloState.IDLE;

    public ArmadilloEntity(GeyserSession session, int entityId, long geyserId, UUID uuid,
            EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void setArmadilloState(ObjectEntityMetadata<ArmadilloState> entityMetadata) {
        armadilloState = entityMetadata.getValue();

        switch (armadilloState) {
            case IDLE -> propertyManager.add("minecraft:armadillo_state", "unrolled");
            case ROLLING -> propertyManager.add("minecraft:armadillo_state", "rolled_up");
            case SCARED -> propertyManager.add("minecraft:armadillo_state", "rolled_up_relaxing");
            case UNROLLING -> propertyManager.add("minecraft:armadillo_state", "rolled_up_unrolling");
        }

        updateBedrockEntityProperties();
    }

    public void onPeeking() {
        // Technically we should wait if not currently scared
        if (armadilloState == ArmadilloState.SCARED) {
            propertyManager.add("minecraft:armadillo_state", "rolled_up_peeking");
            updateBedrockEntityProperties();

            // Needed for consecutive peeks
            session.scheduleInEventLoop(() -> {
                if (armadilloState == ArmadilloState.SCARED) {
                    propertyManager.add("minecraft:armadillo_state", "rolled_up_relaxing");
                    updateBedrockEntityProperties();
                }
            }, 250, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    @Nullable
    protected ItemTag getFoodTag() {
        return ItemTag.ARMADILLO_FOOD;
    }
}
