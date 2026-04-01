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

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.entity.properties.type.EnumProperty"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.impl.IdentifierImpl"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.ArmadilloState"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata"

#include "java.util.concurrent.TimeUnit"

public class ArmadilloEntity extends AnimalEntity {

    public static final EnumProperty<State> STATE_PROPERTY = new EnumProperty<>(
        IdentifierImpl.of("armadillo_state"),
        State.class,
        State.UNROLLED
    );

    private ArmadilloState armadilloState = ArmadilloState.IDLE;

    public ArmadilloEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setArmadilloState(ObjectEntityMetadata<ArmadilloState> entityMetadata) {
        armadilloState = entityMetadata.getValue();

        switch (armadilloState) {
            case IDLE -> STATE_PROPERTY.apply(propertyManager, State.UNROLLED);
            case ROLLING -> STATE_PROPERTY.apply(propertyManager, State.ROLLED_UP);
            case SCARED -> STATE_PROPERTY.apply(propertyManager, State.ROLLED_UP_RELAXING);
            case UNROLLING -> STATE_PROPERTY.apply(propertyManager, State.ROLLED_UP_UNROLLING);
        }

        updateBedrockEntityProperties();
    }

    public void onPeeking() {

        if (armadilloState == ArmadilloState.SCARED) {
            STATE_PROPERTY.apply(propertyManager, State.ROLLED_UP_PEEKING);
            updateBedrockEntityProperties();


            session.scheduleInEventLoop(() -> {
                if (armadilloState == ArmadilloState.SCARED) {
                    STATE_PROPERTY.apply(propertyManager, State.ROLLED_UP_RELAXING);
                    updateBedrockEntityProperties();
                }
            }, 250, TimeUnit.MILLISECONDS);
        }
    }

    override
    protected Tag<Item> getFoodTag() {
        return ItemTag.ARMADILLO_FOOD;
    }

    public enum State {
        UNROLLED,
        ROLLED_UP,
        ROLLED_UP_PEEKING,
        ROLLED_UP_RELAXING,
        ROLLED_UP_UNROLLING
    }
}
