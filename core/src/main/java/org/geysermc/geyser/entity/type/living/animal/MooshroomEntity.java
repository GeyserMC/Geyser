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

package org.geysermc.geyser.entity.type.living.animal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.living.animal.farm.CowEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

public class MooshroomEntity extends CowEntity {
    private boolean isBrown = false;

    public MooshroomEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setMooshroomVariant(IntEntityMetadata metadata) {
        isBrown = metadata.getPrimitiveValue() == 1;
        dirtyMetadata.put(EntityDataTypes.VARIANT, metadata.getPrimitiveValue());
    }

    @Override
    public void addAdditionalSpawnData(AddEntityPacket addEntityPacket) {
        // There are no variants for mooshroom cows, so far
    }

    @NonNull
    @Override
    protected InteractiveTag testMobInteraction(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        if (!isBaby()) {
            if (itemInHand.is(Items.BOWL)) {
                // Stew
                return InteractiveTag.MOOSHROOM_MILK_STEW;
            } else if (isAlive() && itemInHand.is(Items.SHEARS)) {
                // Shear items
                return InteractiveTag.MOOSHROOM_SHEAR;
            }
        }
        return super.testMobInteraction(hand, itemInHand);
    }

    @NonNull
    @Override
    protected InteractionResult mobInteract(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        boolean isBaby = isBaby();
        if (!isBaby && itemInHand.is(Items.BOWL)) {
            // Stew
            return InteractionResult.SUCCESS;
        } else if (!isBaby && isAlive() && itemInHand.is(Items.SHEARS)) {
            // Shear items
            return InteractionResult.SUCCESS;
        } else if (isBrown && itemInHand.is(session, ItemTag.SMALL_FLOWERS)) {
            // ?
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(hand, itemInHand);
    }
}
