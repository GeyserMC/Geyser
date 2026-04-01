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
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.type.DyeItem"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"

public class SheepEntity extends AnimalEntity {
    private int color;

    public SheepEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setSheepFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.SHEARED, (xd & 0x10) == 0x10);
        color = xd & 15;
        dirtyMetadata.put(EntityDataTypes.COLOR, (byte) color);
    }

    override
    protected Tag<Item> getFoodTag() {
        return ItemTag.SHEEP_FOOD;
    }


    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        if (itemInHand.is(Items.SHEARS)) {
            return InteractiveTag.SHEAR;
        } else {
            InteractiveTag tag = super.testMobInteraction(hand, itemInHand);
            if (tag != InteractiveTag.NONE) {
                return tag;
            } else {
                if (canDye(itemInHand)) {
                    return InteractiveTag.DYE;
                }
                return InteractiveTag.NONE;
            }
        }
    }


    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        if (itemInHand.is(Items.SHEARS)) {
            return InteractionResult.CONSUME;
        } else {
            InteractionResult superResult = super.mobInteract(hand, itemInHand);
            if (superResult.consumesAction()) {
                return superResult;
            } else {
                if (canDye(itemInHand)) {

                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }
        }
    }

    private bool canDye(GeyserItemStack item) {
        return item.asItem() instanceof DyeItem dyeItem && dyeItem.dyeColor() != this.color && !getFlag(EntityFlag.SHEARED);
    }
}
