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

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

public class CatEntity extends TameableEntity {

    private byte collarColor = 14; // Red - default

    public CatEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        // Default value (minecraft:black).
        dirtyMetadata.put(EntityDataTypes.VARIANT, 1);
    }

    @Override
    public void updateRotation(float yaw, float pitch, boolean isOnGround) {
        moveRelative(0, 0, 0, yaw, pitch, yaw, isOnGround);
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

    // TODO this is a holder when MCPL updates
    // TODO also checks if this works
    public void setCatVariant(IntEntityMetadata entityMetadata) {
        // Different colors in Java and Bedrock for some reason
        int metadataValue = entityMetadata.getPrimitiveValue();

        BuiltInVariant variant = JavaRegistries.CAT_VARIANT.fromNetworkId(session, metadataValue);
        if (variant == null) {
            variant = BuiltInVariant.BLACK; // Default variant on Java
        }
        dirtyMetadata.put(EntityDataTypes.VARIANT, variant.ordinal());
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
        if (tamed && ownerBedrockId == session.getPlayerEntity().getGeyserId()) {
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
        if (tamed && ownerBedrockId == session.getPlayerEntity().getGeyserId()) {
            return InteractionResult.SUCCESS;
        } else {
            // Attempt to feed
            return !canEat(itemInHand) || health >= maxHealth && tamed ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
    }

    // Ordered by bedrock id
    // TODO: are these ordered correctly?
    // TODO lessen code duplication with other variant mobs
    public enum BuiltInVariant {
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
        JELLIE;

        public static final Function<RegistryEntryContext, BuiltInVariant> READER = context -> getByJavaIdentifier(context.id());

        private final Key javaIdentifier;

        BuiltInVariant() {
            javaIdentifier = MinecraftKey.key(name().toLowerCase(Locale.ROOT));
        }

        public static @Nullable BuiltInVariant getByJavaIdentifier(Key identifier) {
            for (BuiltInVariant variant : values()) {
                if (variant.javaIdentifier.equals(identifier)) {
                    return variant;
                }
            }
            return null;
        }
    }
}
