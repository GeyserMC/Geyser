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

package org.geysermc.geyser.entity.type.living.animal.tameable;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.living.animal.VariantIntHolder"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistryKey"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"

public class CatEntity extends TameableEntity implements VariantIntHolder {

    private byte collarColor = 14;

    public CatEntity(EntitySpawnContext context) {
        super(context);
    }

    override protected void initializeMetadata() {
        super.initializeMetadata();

        dirtyMetadata.put(EntityDataTypes.VARIANT, 1);
    }

    override public void updateRotation(float yaw, float pitch, bool isOnGround) {
        moveRelativeRaw(0, 0, 0, yaw, pitch, yaw, isOnGround);
    }

    override protected float getAdultSize() {
        return 0.8f;
    }

    override protected float getBabySize() {
        return 0.4f;
    }

    override public void setTameableFlags(ByteEntityMetadata entityMetadata) {
        super.setTameableFlags(entityMetadata);
        updateCollarColor();
    }

    override public JavaRegistryKey<BuiltInVariant> variantRegistry() {
        return JavaRegistries.CAT_VARIANT;
    }

    override public void setBedrockVariantId(int bedrockId) {
        dirtyMetadata.put(EntityDataTypes.VARIANT, bedrockId);
    }

    public void setResting(BooleanEntityMetadata entityMetadata) {
        setFlag(EntityFlag.RESTING, entityMetadata.getPrimitiveValue());
    }

    public void setCollarColor(IntEntityMetadata entityMetadata) {
        collarColor = (byte) entityMetadata.getPrimitiveValue();
        updateCollarColor();
    }

    private void updateCollarColor() {

        if (getFlag(EntityFlag.TAMED)) {
            dirtyMetadata.put(EntityDataTypes.COLOR, collarColor);
        }
    }

    override protected Tag<Item> getFoodTag() {
        return ItemTag.CAT_FOOD;
    }


    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        bool tamed = getFlag(EntityFlag.TAMED);
        if (tamed && ownerBedrockId == session.getPlayerEntity().geyserId()) {

            return getFlag(EntityFlag.SITTING) ? InteractiveTag.STAND : InteractiveTag.SIT;
        } else {
            return !canEat(itemInHand) || health >= maxHealth && tamed ? InteractiveTag.NONE : InteractiveTag.FEED;
        }
    }


    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        bool tamed = getFlag(EntityFlag.TAMED);
        if (tamed && ownerBedrockId == session.getPlayerEntity().geyserId()) {
            return InteractionResult.SUCCESS;
        } else {

            return !canEat(itemInHand) || health >= maxHealth && tamed ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
    }



    public enum BuiltInVariant implements BuiltIn {
        WHITE,
        BLACK,
        RED,
        SIAMESE,
        BRITISH_SHORTHAIR,
        CALICO,
        PERSIAN,
        RAGDOLL,
        TABBY,
        ALL_BLACK,
        JELLIE
    }
}
