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
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.enchantment.EnchantmentComponent;
import org.geysermc.geyser.item.type.DyeItem;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.geyser.util.ItemUtils;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.WolfVariant;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;

import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

public class WolfEntity extends TameableEntity {
    private byte collarColor = 14; // Red - default
    private HolderSet repairableItems = null;
    private boolean isCurseOfBinding = false;

    public WolfEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    public void setTameableFlags(ByteEntityMetadata entityMetadata) {
        super.setTameableFlags(entityMetadata);
        // Reset wolf color
        if (getFlag(EntityFlag.ANGRY)) {
            dirtyMetadata.put(EntityDataTypes.COLOR, (byte) 0);
        } else if (getFlag(EntityFlag.TAMED)) {
            updateCollarColor();

            // This fixes tail angle when taming
            UpdateAttributesPacket packet = new UpdateAttributesPacket();
            packet.setRuntimeEntityId(geyserId);
            packet.setAttributes(Collections.singletonList(createHealthAttribute()));
            session.sendUpstreamPacket(packet);
        }
    }

    public void setCollarColor(IntEntityMetadata entityMetadata) {
        collarColor = (byte) entityMetadata.getPrimitiveValue();
        if (!getFlag(EntityFlag.ANGRY) && getFlag(EntityFlag.TAMED)) {
            updateCollarColor();
        }
    }

    private void updateCollarColor() {
        dirtyMetadata.put(EntityDataTypes.COLOR, collarColor);
        if (ownerBedrockId == 0) {
            // If a color is set and there is no owner entity ID, set one.
            // Otherwise, the entire wolf is set to that color: https://user-images.githubusercontent.com/9083212/99209989-92691200-2792-11eb-911d-9a315c955be9.png
            dirtyMetadata.put(EntityDataTypes.OWNER_EID, session.getPlayerEntity().getGeyserId());
        }
    }

    // 1.16+
    public void setWolfAngerTime(IntEntityMetadata entityMetadata) {
        int time = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.ANGRY, time != 0);
        dirtyMetadata.put(EntityDataTypes.COLOR, time != 0 ? (byte) 0 : collarColor);
    }

    // 1.20.5+
    public void setWolfVariant(ObjectEntityMetadata<Holder<WolfVariant>> entityMetadata) {
        entityMetadata.getValue().ifId(id -> {
            BuiltInWolfVariant wolfVariant = session.getRegistryCache().wolfVariants().byId(id);
            if (wolfVariant == null) {
                wolfVariant = BuiltInWolfVariant.PALE;
            }
            dirtyMetadata.put(EntityDataTypes.VARIANT, wolfVariant.ordinal());
        });
    }

    @Override
    @Nullable
    protected Tag<Item> getFoodTag() {
        return ItemTag.WOLF_FOOD;
    }

    @Override
    public void setBody(ItemStack stack) {
        super.setBody(stack);
        isCurseOfBinding = ItemUtils.hasEffect(session, stack, EnchantmentComponent.PREVENT_ARMOR_CHANGE);
        // Not using ItemStack#getDataComponents as that wouldn't include default item components
        repairableItems = GeyserItemStack.from(stack).getComponent(DataComponentType.REPAIRABLE);
    }

    @Override
    public boolean canBeLeashed() {
        return !getFlag(EntityFlag.ANGRY) && super.canBeLeashed();
    }

    @NonNull
    @Override
    protected InteractiveTag testMobInteraction(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        if (getFlag(EntityFlag.ANGRY)) {
            return InteractiveTag.NONE;
        }
        if (itemInHand.asItem() == Items.BONE && !getFlag(EntityFlag.TAMED)) {
            // Bone and untamed - can tame
            return InteractiveTag.TAME;
        }
        if (getFlag(EntityFlag.TAMED) && ownerBedrockId == session.getPlayerEntity().getGeyserId()) {
            if (itemInHand.asItem() instanceof DyeItem dyeItem) {
                // If this fails, as of Java Edition 1.18.1, you cannot toggle sit/stand
                if (dyeItem.dyeColor() != this.collarColor) {
                    return InteractiveTag.DYE;
                } else {
                    return super.testMobInteraction(hand, itemInHand);
                }
            }
            if (itemInHand.asItem() == Items.WOLF_ARMOR && !this.body.isValid() && !getFlag(EntityFlag.BABY)) {
                return InteractiveTag.EQUIP_WOLF_ARMOR;
            }
            if (itemInHand.asItem() == Items.SHEARS && this.body.isValid()
                    && (!isCurseOfBinding || session.getGameMode().equals(GameMode.CREATIVE))) {
                return InteractiveTag.REMOVE_WOLF_ARMOR;
            }
            if (getFlag(EntityFlag.SITTING) &&
                    session.getTagCache().isItem(repairableItems, itemInHand.asItem()) &&
                    this.body.isValid() && this.body.getTag() != null &&
                    this.body.getTag().getInt("Damage") > 0) {
                return InteractiveTag.REPAIR_WOLF_ARMOR;
            }
            // Tamed and owned by player - can sit/stand
            return getFlag(EntityFlag.SITTING) ? InteractiveTag.STAND : InteractiveTag.SIT;
        }
        return super.testMobInteraction(hand, itemInHand);
    }

    @NonNull
    @Override
    protected InteractionResult mobInteract(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        if (ownerBedrockId == session.getPlayerEntity().getGeyserId() || getFlag(EntityFlag.TAMED)
                || itemInHand.asItem() == Items.BONE && !getFlag(EntityFlag.ANGRY)) {
            // Sitting toggle or feeding; not angry
            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.PASS;
        }
    }

    // Ordered by bedrock id
    public enum BuiltInWolfVariant {
        PALE,
        ASHEN,
        BLACK,
        CHESTNUT,
        RUSTY,
        SNOWY,
        SPOTTED,
        STRIPED,
        WOODS;

        private static final BuiltInWolfVariant[] VALUES = values();

        private final String javaIdentifier;

        BuiltInWolfVariant() {
            this.javaIdentifier = "minecraft:" + this.name().toLowerCase(Locale.ROOT);
        }

        public static @Nullable BuiltInWolfVariant getByJavaIdentifier(String javaIdentifier) {
            for (BuiltInWolfVariant wolfVariant : VALUES) {
                if (wolfVariant.javaIdentifier.equals(javaIdentifier)) {
                    return wolfVariant;
                }
            }
            return null;
        }
    }
}
