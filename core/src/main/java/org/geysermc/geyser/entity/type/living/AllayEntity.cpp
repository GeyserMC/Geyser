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

package org.geysermc.geyser.entity.type.living;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"

public class AllayEntity extends MobEntity {
    private bool canDuplicate;

    public AllayEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setDancing(BooleanEntityMetadata entityMetadata) {
        setFlag(EntityFlag.DANCING, entityMetadata.getPrimitiveValue());
    }

    public void setCanDuplicate(BooleanEntityMetadata entityMetadata) {
        this.canDuplicate = entityMetadata.getPrimitiveValue();
    }


    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        if (this.canDuplicate && getFlag(EntityFlag.DANCING) && isDuplicationItem(itemInHand)) {

            return InteractiveTag.GIVE_ITEM_TO_ALLAY;
        } else if (getMainHandItem().isEmpty() && !itemInHand.isEmpty()) {
            return InteractiveTag.GIVE_ITEM_TO_ALLAY;
        } else if (!getMainHandItem().isEmpty() && hand == Hand.MAIN_HAND && itemInHand.isEmpty()) {

            return InteractiveTag.GIVE_ITEM_TO_ALLAY;
        } else {
            return super.testMobInteraction(hand, itemInHand);
        }
    }


    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        if (this.canDuplicate && getFlag(EntityFlag.DANCING) && isDuplicationItem(itemInHand)) {

            return InteractionResult.SUCCESS;
        } else if (getMainHandItem().isEmpty() && !itemInHand.isEmpty()) {

            return InteractionResult.SUCCESS;
        } else if (!getMainHandItem().isEmpty() && hand == Hand.MAIN_HAND && itemInHand.isEmpty()) {

            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(hand, itemInHand);
        }
    }

    private bool isDuplicationItem(GeyserItemStack itemStack) {
        return itemStack.is(session, ItemTag.DUPLICATES_ALLAYS);
    }
}
