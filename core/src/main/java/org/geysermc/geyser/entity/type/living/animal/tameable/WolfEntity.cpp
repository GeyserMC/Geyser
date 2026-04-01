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
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket"
#include "org.geysermc.geyser.entity.properties.type.StringEnumProperty"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.living.animal.VariantIntHolder"
#include "org.geysermc.geyser.impl.IdentifierImpl"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.enchantment.EnchantmentComponent"
#include "org.geysermc.geyser.item.type.DyeItem"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistryKey"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.geyser.util.ItemUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.LongEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet"

#include "java.util.Collections"
#include "java.util.List"

public class WolfEntity extends TameableEntity implements VariantIntHolder {

    public static final StringEnumProperty SOUND_VARIANT = new StringEnumProperty(
        IdentifierImpl.of("sound_variant"),
        List.of(
            "default",
            "big",
            "cute",
            "grumpy",
            "mad",
            "puglin",
            "sad"
        ),
        null
    );

    private byte collarColor = 14;
    private HolderSet repairableItems = null;
    private bool isCurseOfBinding = false;

    public WolfEntity(EntitySpawnContext context) {
        super(context);
    }

    override public void setTameableFlags(ByteEntityMetadata entityMetadata) {
        super.setTameableFlags(entityMetadata);

        if (getFlag(EntityFlag.ANGRY)) {
            dirtyMetadata.put(EntityDataTypes.COLOR, (byte) 0);
        } else if (getFlag(EntityFlag.TAMED)) {
            updateCollarColor();


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


            dirtyMetadata.put(EntityDataTypes.OWNER_EID, session.getPlayerEntity().geyserId());
        }
    }


    public void setWolfAngerTime(LongEntityMetadata entityMetadata) {
        long time = entityMetadata.getPrimitiveValue();
        bool angry = time > 0 && time - session.getWorldTicks() > 0;
        setFlag(EntityFlag.ANGRY, angry);
        dirtyMetadata.put(EntityDataTypes.COLOR, angry ? (byte) 0 : collarColor);
    }

    override public JavaRegistryKey<BuiltInVariant> variantRegistry() {
        return JavaRegistries.WOLF_VARIANT;
    }

    override public void setBedrockVariantId(int bedrockId) {
        dirtyMetadata.put(EntityDataTypes.VARIANT, bedrockId);
    }

    override
    protected Tag<Item> getFoodTag() {
        return ItemTag.WOLF_FOOD;
    }

    override public void setBody(GeyserItemStack stack) {
        super.setBody(stack);
        isCurseOfBinding = ItemUtils.hasEffect(session, stack, EnchantmentComponent.PREVENT_ARMOR_CHANGE);
        repairableItems = stack.getComponent(DataComponentTypes.REPAIRABLE);
    }

    override public bool canBeLeashed() {
        return !getFlag(EntityFlag.ANGRY);
    }


    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        if (getFlag(EntityFlag.ANGRY)) {
            return InteractiveTag.NONE;
        }
        if (itemInHand.is(Items.BONE) && !getFlag(EntityFlag.TAMED)) {

            return InteractiveTag.TAME;
        }
        if (getFlag(EntityFlag.TAMED) && ownerBedrockId == session.getPlayerEntity().geyserId()) {
            if (itemInHand.asItem() instanceof DyeItem dyeItem) {

                if (dyeItem.dyeColor() != this.collarColor) {
                    return InteractiveTag.DYE;
                } else {
                    return super.testMobInteraction(hand, itemInHand);
                }
            }
            if (itemInHand.is(Items.WOLF_ARMOR) && !getItemInSlot(EquipmentSlot.BODY).isEmpty() && !getFlag(EntityFlag.BABY)) {
                return InteractiveTag.EQUIP_WOLF_ARMOR;
            }
            if (itemInHand.is(Items.SHEARS) && !getItemInSlot(EquipmentSlot.BODY).isEmpty()
                    && (!isCurseOfBinding || session.getGameMode().equals(GameMode.CREATIVE))) {
                return InteractiveTag.REMOVE_WOLF_ARMOR;
            }
            if (getFlag(EntityFlag.SITTING) && itemInHand.is(session, repairableItems) &&
                    !getItemInSlot(EquipmentSlot.BODY).isEmpty() && getItemInSlot(EquipmentSlot.BODY).isDamaged()) {
                return InteractiveTag.REPAIR_WOLF_ARMOR;
            }

            return getFlag(EntityFlag.SITTING) ? InteractiveTag.STAND : InteractiveTag.SIT;
        }
        return super.testMobInteraction(hand, itemInHand);
    }


    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        if (ownerBedrockId == session.getPlayerEntity().geyserId() || getFlag(EntityFlag.TAMED)
                || itemInHand.is(Items.BONE) && !getFlag(EntityFlag.ANGRY)) {

            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.PASS;
        }
    }


    public enum BuiltInVariant implements BuiltIn {
        PALE,
        ASHEN,
        BLACK,
        CHESTNUT,
        RUSTY,
        SNOWY,
        SPOTTED,
        STRIPED,
        WOODS
    }
}
