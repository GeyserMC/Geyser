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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.living.animal.VariantIntHolder;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

public class CatEntity extends TameableEntity implements VariantIntHolder {

    private byte collarColor = 14; // Red - default

    public CatEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        // Default value (minecraft:black).
        dirtyMetadata.put(EntityDataTypes.VARIANT, 1);
    }

    @Override
    public void updateRotation(float yaw, float pitch, boolean isOnGround) {
        moveRelativeRaw(0, 0, 0, yaw, pitch, yaw, isOnGround);
    }

    @Override
    protected float getAdultSize() {
        return 0.8f;
    }

    @Override
    protected float getBabySize() {
        return 0.4f;
    }

    @Override
    public void setTameableFlags(ByteEntityMetadata entityMetadata) {
        super.setTameableFlags(entityMetadata);
        updateCollarColor();
    }

    @Override
    public JavaRegistryKey<BuiltInVariant> variantRegistry() {
        return JavaRegistries.CAT_VARIANT;
    }

    @Override
    public void setBedrockVariantId(int bedrockId) {
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
        // Needed or else wild cats are a red color
        if (getFlag(EntityFlag.TAMED)) {
            dirtyMetadata.put(EntityDataTypes.COLOR, collarColor);
        }
    }

    @Override
    protected @Nullable Tag<Item> getFoodTag() {
        return ItemTag.CAT_FOOD;
    }

    @NonNull
    @Override
    protected InteractiveTag testMobInteraction(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        boolean tamed = getFlag(EntityFlag.TAMED);
        if (tamed && ownerBedrockId == session.getPlayerEntity().geyserId()) {
            // Toggle sitting
            return getFlag(EntityFlag.SITTING) ? InteractiveTag.STAND : InteractiveTag.SIT;
        } else {
            return !canEat(itemInHand) || health >= maxHealth && tamed ? InteractiveTag.NONE : InteractiveTag.FEED;
        }
    }

    @NonNull
    @Override
    protected InteractionResult mobInteract(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        boolean tamed = getFlag(EntityFlag.TAMED);
        if (tamed && ownerBedrockId == session.getPlayerEntity().geyserId()) {
            return InteractionResult.SUCCESS;
        } else {
            // Attempt to feed
            return !canEat(itemInHand) || health >= maxHealth && tamed ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
    }

    // Ordered by bedrock id
    // TODO: are these ordered correctly?
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
