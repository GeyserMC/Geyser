/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.living.animal.nautilus;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.living.animal.tameable.TameableEntity;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.entity.vehicle.NautilusVehicleComponent;
import org.geysermc.geyser.entity.vehicle.VehicleComponent;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.enchantment.EnchantmentComponent;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.geyser.util.ItemUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;

import java.util.UUID;

public abstract class AbstractNautilusEntity extends TameableEntity implements ClientVehicle {
    private HolderSet repairableItems = null;
    private boolean isCurseOfBinding = false;
    private final NautilusVehicleComponent vehicleComponent;

    public AbstractNautilusEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw, float defSpeed) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
        this.vehicleComponent = new NautilusVehicleComponent(this, 0.0f, defSpeed);

        dirtyMetadata.put(EntityDataTypes.CONTAINER_SIZE, 2);
    }

    @Override
    protected @Nullable Tag<Item> getFoodTag() {
        return ItemTag.NAUTILUS_FOOD;
    }

    @Override
    public void setBody(GeyserItemStack stack) {
        super.setBody(stack);
        isCurseOfBinding = ItemUtils.hasEffect(session, stack, EnchantmentComponent.PREVENT_ARMOR_CHANGE);
        repairableItems = stack.getComponent(DataComponentTypes.REPAIRABLE);
    }

    @NonNull
    @Override
    protected InteractiveTag testMobInteraction(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        if (getFlag(EntityFlag.ANGRY)) {
            return InteractiveTag.NONE;
        }

        if (getFlag(EntityFlag.TAMED)) {
            if (itemInHand.asItem().javaIdentifier().endsWith("_nautilus_armor") && !getItemInSlot(EquipmentSlot.BODY).isEmpty() && !getFlag(EntityFlag.BABY)) {
                return InteractiveTag.EQUIP_WOLF_ARMOR; // TODO
            }
            if (itemInHand.is(Items.SHEARS) && !getItemInSlot(EquipmentSlot.BODY).isEmpty()
                    && (!isCurseOfBinding || session.getGameMode().equals(GameMode.CREATIVE))) {
                return InteractiveTag.REMOVE_WOLF_ARMOR; // TODO
            }
            if (itemInHand.is(session, repairableItems) &&
                    !getItemInSlot(EquipmentSlot.BODY).isEmpty() && getItemInSlot(EquipmentSlot.BODY).isDamaged()) {
                return InteractiveTag.REPAIR_WOLF_ARMOR; // TODO
            }
        } else if (getFlag(EntityFlag.BABY) || getFlag(EntityFlag.TAMED)) {
            if (itemInHand.is(session, ItemTag.NAUTILUS_FOOD)) {
                // Can feed either baby nautilus, or tamed nautilus
                return InteractiveTag.FEED;
            }
        } else {
            if (itemInHand.is(session, ItemTag.NAUTILUS_TAMING_ITEMS)) {
                // Nautilus taming food and untamed - can tame
                return InteractiveTag.TAME;
            }
        }
        return super.testMobInteraction(hand, itemInHand);
    }

    public void setBoost(IntEntityMetadata entityMetadata) {
        vehicleComponent.startBoost(entityMetadata.getPrimitiveValue());
    }

    @Override
    public VehicleComponent<?> getVehicleComponent() {
        return vehicleComponent;
    }

    @Override
    public Vector3f getRiddenInput(Vector2f input) {
        float x = input.getX();
        float y = 0.0f;
        float z = 0.0f;

        if (input.getY() != 0.0f) {
            float pitch = session.getPlayerEntity().getPitch();
            z = TrigMath.cos(pitch * TrigMath.DEG_TO_RAD);
            y = -TrigMath.sin(pitch * TrigMath.DEG_TO_RAD);
            if (input.getY() < 0.0f) {
                z *= -0.5f;
                y *= -0.5f;
            }
        }

        if (session.getInputCache().wasJumping()) {
            y += 0.5f;
        }

        return Vector3f.from(x, y, z).mul(3.9f * vehicleComponent.getMoveSpeed());
    }

    @Override
    public float getVehicleSpeed() {
        return 0.0f; // Unused
    }

    @Override
    public boolean isClientControlled() {
        return false;
    }

    @Override
    public boolean canClimb() {
        return false;
    }
}
