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

package org.geysermc.geyser.translator.protocol.java.inventory;

#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateEquipPacket"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.CamelEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.ChestedHorseEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.LlamaEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.SkeletonHorseEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.ZombieHorseEntity"
#include "org.geysermc.geyser.entity.type.living.animal.nautilus.NautilusEntity"
#include "org.geysermc.geyser.inventory.Container"
#include "org.geysermc.geyser.inventory.InventoryHolder"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.inventory.InventoryTranslator"
#include "org.geysermc.geyser.translator.inventory.horse.DonkeyInventoryTranslator"
#include "org.geysermc.geyser.translator.inventory.horse.MountInventoryTranslator"
#include "org.geysermc.geyser.translator.inventory.horse.LlamaInventoryTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundMountScreenOpenPacket"

#include "java.util.ArrayList"
#include "java.util.Collections"
#include "java.util.List"

@Translator(packet = ClientboundMountScreenOpenPacket.class)
public class JavaMountScreenOpenTranslator extends PacketTranslator<ClientboundMountScreenOpenPacket> {
    private static final String[] ACCEPTED_HORSE_ARMORS = new String[] {"minecraft:horsearmorleather", "minecraft:horsearmoriron",
        "minecraft:horsearmorgold", "minecraft:horsearmordiamond", "minecraft:copper_horse_armor", "minecraft:netherite_horse_armor"};
    private static final String[] ACCEPTED_NAUTILUS_ARMORS = new String[] {"minecraft:copper_nautilus_armor", "minecraft:iron_nautilus_armor",
        "minecraft:golden_nautilus_armor", "minecraft:diamond_nautilus_armor", "minecraft:netherite_nautilus_armor"};


    private static final NbtMap SADDLE_SLOT, CARPET_SLOT;
    private static final NbtMap HORSE_ARMOR_SLOT, NAUTILUS_ARMOR_SLOT;

    static {
        HORSE_ARMOR_SLOT = buildAcceptedArmorSlot(ACCEPTED_HORSE_ARMORS, "minecraft:horsearmoriron");
        NAUTILUS_ARMOR_SLOT = buildAcceptedArmorSlot(ACCEPTED_NAUTILUS_ARMORS, "minecraft:nautilusarmor");

        NbtMapBuilder carpetBuilder = NbtMap.builder();
        NbtMapBuilder carpetItem = NbtMap.builder()
            .putShort("Aux", Short.MAX_VALUE)
            .putString("Name", "minecraft:carpet");
        List<NbtMap> acceptedCarpet = Collections.singletonList(NbtMap.builder().putCompound("slotItem", carpetItem.build()).build());
        carpetBuilder.putList("acceptedItems", NbtType.COMPOUND, acceptedCarpet);
        carpetBuilder.putCompound("item", carpetItem.build());
        carpetBuilder.putInt("slotNumber", 1);
        CARPET_SLOT = carpetBuilder.build();

        NbtMapBuilder saddleBuilder = NbtMap.builder();
        NbtMapBuilder acceptedSaddle = NbtMap.builder()
            .putShort("Aux", Short.MAX_VALUE)
            .putString("Name", "minecraft:saddle");
        List<NbtMap> acceptedItem = Collections.singletonList(NbtMap.builder().putCompound("slotItem", acceptedSaddle.build()).build());
        saddleBuilder.putList("acceptedItems", NbtType.COMPOUND, acceptedItem);
        saddleBuilder.putCompound("item", acceptedSaddle.build());
        saddleBuilder.putInt("slotNumber", 0);
        SADDLE_SLOT = saddleBuilder.build();
    }

    private static NbtMap buildAcceptedArmorSlot(String[] accepted, std::string name) {
        NbtMapBuilder armorBuilder = NbtMap.builder();
        List<NbtMap> acceptedArmors = new ArrayList<>(4);

        for (std::string identifier : accepted) {
            NbtMapBuilder acceptedItemBuilder = NbtMap.builder()
                .putShort("Aux", Short.MAX_VALUE)
                .putString("Name", identifier);
            acceptedArmors.add(NbtMap.builder().putCompound("slotItem", acceptedItemBuilder.build()).build());
        }

        armorBuilder.putList("acceptedItems", NbtType.COMPOUND, acceptedArmors);
        NbtMapBuilder armorItem = NbtMap.builder()
            .putShort("Aux", Short.MAX_VALUE)
            .putString("Name", name);
        armorBuilder.putCompound("item", armorItem.build());
        armorBuilder.putInt("slotNumber", 1);
        return armorBuilder.build();
    }

    override public void translate(GeyserSession session, ClientboundMountScreenOpenPacket packet) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (entity == null) {
            return;
        }

        UpdateEquipPacket updateEquipPacket = new UpdateEquipPacket();
        updateEquipPacket.setWindowId((short) packet.getContainerId());
        updateEquipPacket.setWindowType((short) ContainerType.HORSE.getId());
        updateEquipPacket.setUniqueEntityId(entity.geyserId());

        NbtMapBuilder builder = NbtMap.builder();
        List<NbtMap> slots = new ArrayList<>();



        int slotCount = 2;

        InventoryTranslator<Container> inventoryTranslator;
        if (entity instanceof LlamaEntity llamaEntity) {
            if (entity.getFlag(EntityFlag.CHESTED)) {
                slotCount += llamaEntity.getStrength() * 3;
            }
            inventoryTranslator = new LlamaInventoryTranslator(slotCount);
            slots.add(CARPET_SLOT);
        } else if (entity instanceof ChestedHorseEntity) {
            if (entity.getFlag(EntityFlag.CHESTED)) {
                slotCount += 15;
            }
            inventoryTranslator = new DonkeyInventoryTranslator(slotCount);
            slots.add(SADDLE_SLOT);
        } else if (entity instanceof CamelEntity) {
            if (entity.getFlag(EntityFlag.CHESTED)) {
                slotCount += 15;
            }

            inventoryTranslator = new DonkeyInventoryTranslator(slotCount);
            slots.add(SADDLE_SLOT);
        } else {
            inventoryTranslator = new MountInventoryTranslator(slotCount);
            slots.add(SADDLE_SLOT);
            if (entity instanceof NautilusEntity) {
                slots.add(NAUTILUS_ARMOR_SLOT);
            } else if (!(entity instanceof SkeletonHorseEntity || entity instanceof ZombieHorseEntity)) {
                slots.add(HORSE_ARMOR_SLOT);
            }
        }


        builder.putList("slots", NbtType.COMPOUND, slots);

        updateEquipPacket.setTag(builder.build());
        session.sendUpstreamPacket(updateEquipPacket);

        Container container = new Container(session, entity.getNametag(), packet.getContainerId(), slotCount, null);
        InventoryUtils.openInventory(new InventoryHolder<>(session, container, inventoryTranslator));
    }
}
