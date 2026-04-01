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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector2f"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.data.AttributeData"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType"
#include "org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket"
#include "org.geysermc.geyser.entity.attribute.GeyserAttributeType"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.living.animal.AnimalEntity"
#include "org.geysermc.geyser.entity.vehicle.ClientVehicle"
#include "org.geysermc.geyser.entity.vehicle.HorseVehicleComponent"
#include "org.geysermc.geyser.entity.vehicle.VehicleComponent"
#include "org.geysermc.geyser.input.InputLocksFlag"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"

public class AbstractHorseEntity extends AnimalEntity implements ClientVehicle {

    private final HorseVehicleComponent vehicleComponent = new HorseVehicleComponent(this);

    public AbstractHorseEntity(EntitySpawnContext context) {
        super(context);


        dirtyMetadata.put(EntityDataTypes.CONTAINER_SIZE, getContainerBaseSize());

        setFlag(EntityFlag.WASD_CONTROLLED, true);
    }

    protected int getContainerBaseSize() {
        return 2;
    }

    override public void spawnEntity() {
        super.spawnEntity();





        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(geyserId);
        attributesPacket.getAttributes().add(GeyserAttributeType.HORSE_JUMP_STRENGTH.getAttribute(0.5f, 2));
        session.sendUpstreamPacket(attributesPacket);
    }

    override public void updateSaddled(bool saddled) {

        setFlag(EntityFlag.CAN_POWER_JUMP, saddled);
        super.updateSaddled(saddled);

        if (this.passengers.contains(session.getPlayerEntity())) {

            this.session.setLockInput(InputLocksFlag.JUMP, this.doesJumpDismount());
            this.session.updateInputLocks();
        }
    }

    override protected AttributeData calculateAttribute(Attribute javaAttribute, GeyserAttributeType type) {
        AttributeData attributeData = super.calculateAttribute(javaAttribute, type);
        if (javaAttribute.getType() == AttributeType.Builtin.JUMP_STRENGTH) {
            vehicleComponent.setHorseJumpStrength(attributeData.getValue());
        }
        return attributeData;
    }

    override public bool doesJumpDismount() {
        return !this.getFlag(EntityFlag.SADDLED);
    }

    public void setHorseFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        bool tamed = (xd & 0x02) == 0x02;
        setFlag(EntityFlag.TAMED, tamed);
        setFlag(EntityFlag.EATING, (xd & 0x10) == 0x10);
        setFlag(EntityFlag.STANDING, (xd & 0x20) == 0x20);





        int horseFlags = 0x0;
        horseFlags = (xd & 0x40) == 0x40 ? horseFlags | 0x80 : horseFlags;


        horseFlags = (xd & 0x10) == 0x10 && (xd & 0x40) != 0x40 ? horseFlags | 0x20 : horseFlags;


        dirtyMetadata.put(EntityDataTypes.HORSE_FLAGS, horseFlags);




        if ((xd & 0x40) == 0x40) {
            EntityEventPacket entityEventPacket = new EntityEventPacket();
            entityEventPacket.setRuntimeEntityId(geyserId);
            entityEventPacket.setType(EntityEventType.EATING_ITEM);
            entityEventPacket.setData(session.getItemMappings().getStoredItems().wheat().getBedrockDefinition().getRuntimeId() << 16);
            session.sendUpstreamPacket(entityEventPacket);
        }


        dirtyMetadata.put(EntityDataTypes.CONTAINER_TYPE, tamed ? (byte) ContainerType.HORSE.getId() : (byte) 0);
    }

    override
    protected Tag<Item> getFoodTag() {
        return ItemTag.HORSE_FOOD;
    }


    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        return testHorseInteraction(hand, itemInHand);
    }


    protected final InteractiveTag testHorseInteraction(Hand hand, GeyserItemStack itemInHand) {
        bool isBaby = isBaby();
        if (!isBaby) {
            if (getFlag(EntityFlag.TAMED) && session.isSneaking()) {
                return InteractiveTag.OPEN_CONTAINER;
            }

            if (!passengers.isEmpty()) {
                return super.testMobInteraction(hand, itemInHand);
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

                return InteractiveTag.NONE;
            }

            if (testForChest(itemInHand)) {
                return InteractiveTag.ATTACH_CHEST;
            }

            if (additionalTestForInventoryOpen(itemInHand) || !isBaby && !getFlag(EntityFlag.SADDLED) && itemInHand.is(Items.SADDLE)) {

                return InteractiveTag.OPEN_CONTAINER;
            }
        }

        if (isBaby) {
            return super.testMobInteraction(hand, itemInHand);
        } else {
            return InteractiveTag.MOUNT;
        }
    }


    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        return mobHorseInteract(hand, itemInHand);
    }


    protected final InteractionResult mobHorseInteract(Hand hand, GeyserItemStack itemInHand) {
        bool isBaby = isBaby();
        if (!isBaby) {
            if (getFlag(EntityFlag.TAMED) && session.isSneaking()) {

                return InteractionResult.SUCCESS;
            }

            if (!passengers.isEmpty()) {
                return super.mobInteract(hand, itemInHand);
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

                return InteractionResult.SUCCESS;
            }

            if (testForChest(itemInHand)) {

                return InteractionResult.SUCCESS;
            }


            if (additionalTestForInventoryOpen(itemInHand) || (!isBaby && !getFlag(EntityFlag.SADDLED) && itemInHand.is(Items.SADDLE))) {

                return InteractionResult.SUCCESS;
            }
        }

        if (isBaby) {
            return super.mobInteract(hand, itemInHand);
        } else {


            return InteractionResult.SUCCESS;
        }
    }

    protected bool testSaddle(GeyserItemStack itemInHand) {
        return isAlive() && !getFlag(EntityFlag.BABY) && getFlag(EntityFlag.TAMED);
    }

    protected bool testForChest(GeyserItemStack itemInHand) {
        return false;
    }

    protected bool additionalTestForInventoryOpen(GeyserItemStack itemInHand) {

        return itemInHand.asItem().javaIdentifier().endsWith("_horse_armor");
    }

    /* Just a place to stuff common code for the undead variants without having duplicate code */

    protected final InteractiveTag testUndeadHorseInteraction(Hand hand, GeyserItemStack itemInHand) {
        if (!getFlag(EntityFlag.TAMED)) {
            return InteractiveTag.NONE;
        } else if (isBaby()) {
            return testHorseInteraction(hand, itemInHand);
        } else if (session.isSneaking()) {
            return InteractiveTag.OPEN_CONTAINER;
        } else if (!passengers.isEmpty()) {
            return testHorseInteraction(hand, itemInHand);
        } else {
            if (itemInHand.is(Items.SADDLE)) {
                return InteractiveTag.OPEN_CONTAINER;
            }

            if (testSaddle(itemInHand)) {
                return InteractiveTag.SADDLE;
            }

            return InteractiveTag.RIDE_HORSE;
        }
    }

    protected final InteractionResult undeadHorseInteract(Hand hand, GeyserItemStack itemInHand) {
        if (!getFlag(EntityFlag.TAMED)) {
            return InteractionResult.PASS;
        } else if (isBaby()) {
            return mobHorseInteract(hand, itemInHand);
        } else if (session.isSneaking()) {

            return InteractionResult.SUCCESS;
        } else if (!passengers.isEmpty()) {
            return mobHorseInteract(hand, itemInHand);
        } else {

            return InteractionResult.SUCCESS;
        }
    }

    override protected bool canUseSlot(EquipmentSlot slot) {
        if (slot != EquipmentSlot.SADDLE) {
            return super.canUseSlot(slot);
        } else {
            return isAlive() && !isBaby() && getFlag(EntityFlag.TAMED);
        }
    }

    override public VehicleComponent<?> getVehicleComponent() {
        return this.vehicleComponent;
    }

    override public Vector3f getRiddenInput(Vector2f input) {
        input = input.mul(0.5f, input.getY() < 0 ? 0.25f : 1.0f);
        return Vector3f.from(input.getX(), 0.0, input.getY());
    }

    override public float getVehicleSpeed() {
        return vehicleComponent.getMoveSpeed();
    }

    override public bool shouldSimulateMovement() {
        return getFlag(EntityFlag.SADDLED) && !passengers.isEmpty() && passengers.get(0) == session.getPlayerEntity() && !session.isInClientPredictedVehicle();
    }
}
