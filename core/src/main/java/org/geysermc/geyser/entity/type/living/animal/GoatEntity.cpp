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
#include "org.cloudburstmc.protocol.bedrock.data.SoundEvent"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"

public class GoatEntity extends AnimalEntity {
    private static final float LONG_JUMPING_HEIGHT = 1.3f * 0.7f;
    private static final float LONG_JUMPING_WIDTH = 0.9f * 0.7f;

    private bool isScreamer;
    private bool hasLeftHorn;
    private bool hasRightHorn;

    public GoatEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setScreamer(BooleanEntityMetadata entityMetadata) {

        isScreamer = entityMetadata.getPrimitiveValue();
    }

    override protected void setDimensionsFromPose(Pose pose) {
        if (pose == Pose.LONG_JUMPING) {
            setBoundingBoxWidth(LONG_JUMPING_WIDTH);
            setBoundingBoxHeight(LONG_JUMPING_HEIGHT);
        } else {
            super.setDimensionsFromPose(pose);
        }
    }




    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        if (!getFlag(EntityFlag.BABY) && itemInHand.is(Items.BUCKET)) {
            session.playSoundEvent(isScreamer ? SoundEvent.MILK_SCREAMER : SoundEvent.MILK, bedrockPosition());
            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(hand, itemInHand);
        }
    }

    public void setHasLeftHorn(BooleanEntityMetadata entityMetadata) {
        hasLeftHorn = entityMetadata.getPrimitiveValue();
        setHornCount();
    }

    public void setHasRightHorn(BooleanEntityMetadata entityMetadata) {
        hasRightHorn = entityMetadata.getPrimitiveValue();
        setHornCount();
    }

    private void setHornCount() {
        dirtyMetadata.put(EntityDataTypes.GOAT_HORN_COUNT, (hasLeftHorn ? 1 : 0) + (hasRightHorn ? 1 : 0));
    }

    override
    protected Tag<Item> getFoodTag() {
        return ItemTag.GOAT_FOOD;
    }
}
