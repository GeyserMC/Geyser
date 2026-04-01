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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.living.animal.farm.CowEntity"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"

public class MooshroomEntity extends CowEntity {
    private bool isBrown = false;

    public MooshroomEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setMooshroomVariant(IntEntityMetadata metadata) {
        isBrown = metadata.getPrimitiveValue() == 1;
        dirtyMetadata.put(EntityDataTypes.VARIANT, metadata.getPrimitiveValue());
    }

    override public void addAdditionalSpawnData(AddEntityPacket addEntityPacket) {

    }


    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        if (!isBaby()) {
            if (itemInHand.is(Items.BOWL)) {

                return InteractiveTag.MOOSHROOM_MILK_STEW;
            } else if (isAlive() && itemInHand.is(Items.SHEARS)) {

                return InteractiveTag.MOOSHROOM_SHEAR;
            }
        }
        return super.testMobInteraction(hand, itemInHand);
    }


    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        bool isBaby = isBaby();
        if (!isBaby && itemInHand.is(Items.BOWL)) {

            return InteractionResult.SUCCESS;
        } else if (!isBaby && isAlive() && itemInHand.is(Items.SHEARS)) {

            return InteractionResult.SUCCESS;
        } else if (isBrown && itemInHand.is(session, ItemTag.SMALL_FLOWERS)) {

            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(hand, itemInHand);
    }
}
