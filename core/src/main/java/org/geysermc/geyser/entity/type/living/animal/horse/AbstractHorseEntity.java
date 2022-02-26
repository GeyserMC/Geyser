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

package org.geysermc.geyser.entity.type.living.animal.horse;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.google.common.collect.ImmutableSet;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.type.living.animal.AnimalEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

public class AbstractHorseEntity extends AnimalEntity {
    /**
     * A list of all foods a horse/donkey can eat on Java Edition.
     * Used to display interactive tag if needed.
     */
    private static final Set<String> DONKEY_AND_HORSE_FOODS = ImmutableSet.of("golden_apple", "enchanted_golden_apple",
            "golden_carrot", "sugar", "apple", "wheat", "hay_block");

    public AbstractHorseEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);

        // Specifies the size of the entity's inventory. Required to place slots in the entity.
        dirtyMetadata.put(EntityData.CONTAINER_BASE_SIZE, getContainerBaseSize());

        setFlag(EntityFlag.WASD_CONTROLLED, true);
    }

    protected int getContainerBaseSize() {
        return 2;
    }

    @Override
    public void spawnEntity() {
        super.spawnEntity();

        // Add horse jump strength attribute to allow donkeys and mules to jump, if they don't send the attribute themselves.
        // Confirmed broken without this code by making a new donkey in vanilla 1.17.1
        // The spawn packet does have an attributes section, but adding the jump strength property there causes the
        // donkey to jump very high.
        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(geyserId);
        attributesPacket.getAttributes().add(GeyserAttributeType.HORSE_JUMP_STRENGTH.getAttribute(0.5f, 2));
        session.sendUpstreamPacket(attributesPacket);
    }

    public void setHorseFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        boolean tamed = (xd & 0x02) == 0x02;
        boolean saddled = (xd & 0x04) == 0x04;
        setFlag(EntityFlag.TAMED, tamed);
        setFlag(EntityFlag.SADDLED, saddled);
        setFlag(EntityFlag.EATING, (xd & 0x10) == 0x10);
        setFlag(EntityFlag.STANDING, (xd & 0x20) == 0x20);

        // HorseFlags
        // Bred 0x10
        // Eating 0x20
        // Open mouth 0x80
        int horseFlags = 0x0;
        horseFlags = (xd & 0x40) == 0x40 ? horseFlags | 0x80 : horseFlags;

        // Only set eating when we don't have mouth open so a player interaction doesn't trigger the eating animation
        horseFlags = (xd & 0x10) == 0x10 && (xd & 0x40) != 0x40 ? horseFlags | 0x20 : horseFlags;

        // Set the flags into the display item
        dirtyMetadata.put(EntityData.DISPLAY_ITEM, horseFlags);

        // Send the eating particles
        // We use the wheat metadata as static particles since Java
        // doesn't send over what item was used to feed the horse
        if ((xd & 0x40) == 0x40) {
            EntityEventPacket entityEventPacket = new EntityEventPacket();
            entityEventPacket.setRuntimeEntityId(geyserId);
            entityEventPacket.setType(EntityEventType.EATING_ITEM);
            entityEventPacket.setData(session.getItemMappings().getStoredItems().wheat().getBedrockId() << 16);
            session.sendUpstreamPacket(entityEventPacket);
        }

        // Set container type if tamed
        dirtyMetadata.put(EntityData.CONTAINER_TYPE, tamed ? (byte) ContainerType.HORSE.getId() : (byte) 0);

        // Shows the jump meter
        setFlag(EntityFlag.CAN_POWER_JUMP, saddled);
    }

    @Override
    public boolean canEat(String javaIdentifierStripped, ItemMapping mapping) {
        return DONKEY_AND_HORSE_FOODS.contains(javaIdentifierStripped);
    }

    @Nonnull
    @Override
    protected InteractiveTag testMobInteraction(@Nonnull GeyserItemStack itemInHand) {
        boolean isBaby = isBaby();
        if (!isBaby) {
            if (getFlag(EntityFlag.TAMED) && session.isSneaking()) {
                return InteractiveTag.OPEN_CONTAINER;
            }

            if (!passengers.isEmpty()) {
                return super.testMobInteraction(itemInHand);
            }
        }

        if (!itemInHand.isEmpty()) {
            if (canEat(itemInHand)) {
                return InteractiveTag.FEED;
            }

            if (testSaddle(itemInHand)) {
                return InteractiveTag.SADDLE;
            }

            if (!getFlag(EntityFlag.TAMED)) {
                // Horse will become mad
                return InteractiveTag.NONE;
            }

            if (testForChest(itemInHand)) {
                return InteractiveTag.ATTACH_CHEST;
            }

            if (additionalTestForInventoryOpen(itemInHand) || !isBaby && !getFlag(EntityFlag.SADDLED) && itemInHand.getJavaId() == session.getItemMappings().getStoredItems().saddle()) {
                // Will open the inventory to be saddled
                return InteractiveTag.OPEN_CONTAINER;
            }
        }

        if (isBaby) {
            return super.testMobInteraction(itemInHand);
        } else {
            return InteractiveTag.MOUNT;
        }
    }

    @Nonnull
    @Override
    protected InteractionResult mobInteract(@Nonnull GeyserItemStack itemInHand) {
        boolean isBaby = isBaby();
        if (!isBaby) {
            if (getFlag(EntityFlag.TAMED) && session.isSneaking()) {
                // Will open the inventory
                return InteractionResult.SUCCESS;
            }

            if (!passengers.isEmpty()) {
                return super.mobInteract(itemInHand);
            }
        }

        if (!itemInHand.isEmpty()) {
            if (canEat(itemInHand)) {
                if (isBaby) {
                    playEntityEvent(EntityEventType.BABY_ANIMAL_FEED);
                }
                return InteractionResult.CONSUME;
            }

            if (testSaddle(itemInHand)) {
                return InteractionResult.SUCCESS;
            }

            if (!getFlag(EntityFlag.TAMED)) {
                // Horse will become mad
                return InteractionResult.SUCCESS;
            }

            if (testForChest(itemInHand)) {
                // TODO looks like chest is also handled client side
                return InteractionResult.SUCCESS;
            }

            // Note: yes, this code triggers for llamas too. lol (as of Java Edition 1.18.1)
            if (additionalTestForInventoryOpen(itemInHand) || (!isBaby && !getFlag(EntityFlag.SADDLED) && itemInHand.getJavaId() == session.getItemMappings().getStoredItems().saddle())) {
                // Will open the inventory to be saddled
                return InteractionResult.SUCCESS;
            }
        }

        if (isBaby) {
            return super.mobInteract(itemInHand);
        } else {
            // Attempt to mount
            // TODO client-set flags sitting standing?
            return InteractionResult.SUCCESS;
        }
    }

    protected boolean testSaddle(@Nonnull GeyserItemStack itemInHand) {
        return isAlive() && !getFlag(EntityFlag.BABY) && getFlag(EntityFlag.TAMED);
    }

    protected boolean testForChest(@Nonnull GeyserItemStack itemInHand) {
        return false;
    }

    protected boolean additionalTestForInventoryOpen(@Nonnull GeyserItemStack itemInHand) {
        return itemInHand.getMapping(session).getJavaIdentifier().endsWith("_horse_armor");
    }

    /* Just a place to stuff common code for the undead variants without having duplicate code */

    protected final InteractiveTag testUndeadHorseInteraction(@Nonnull GeyserItemStack itemInHand) {
        if (!getFlag(EntityFlag.TAMED)) {
            return InteractiveTag.NONE;
        } else if (isBaby()) {
            return testMobInteraction(itemInHand);
        } else if (session.isSneaking()) {
            return InteractiveTag.OPEN_CONTAINER;
        } else if (!passengers.isEmpty()) {
            return testMobInteraction(itemInHand);
        } else {
            if (session.getItemMappings().getStoredItems().saddle() == itemInHand.getJavaId()) {
                return InteractiveTag.OPEN_CONTAINER;
            }

            if (testSaddle(itemInHand)) {
                return InteractiveTag.SADDLE;
            }

            return InteractiveTag.RIDE_HORSE;
        }
    }

    protected final InteractionResult undeadHorseInteract(@Nonnull GeyserItemStack itemInHand) {
        if (!getFlag(EntityFlag.TAMED)) {
            return InteractionResult.PASS;
        } else if (isBaby()) {
            return mobInteract(itemInHand);
        } else if (session.isSneaking()) {
            // Opens inventory
            return InteractionResult.SUCCESS;
        } else if (!passengers.isEmpty()) {
            return mobInteract(itemInHand);
        } else {
            // The client tests for saddle but it doesn't matter for us at this point.
            return InteractionResult.SUCCESS;
        }
    }
}
