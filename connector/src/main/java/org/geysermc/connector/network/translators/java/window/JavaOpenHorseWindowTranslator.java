/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.java.window;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenHorseWindowPacket;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.UpdateEquipPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.living.animal.horse.ChestedHorseEntity;
import org.geysermc.connector.entity.living.animal.horse.LlamaEntity;
import org.geysermc.connector.inventory.Container;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.horse.DonkeyInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.horse.HorseInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.horse.LlamaInventoryTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Translator(packet = ServerOpenHorseWindowPacket.class)
public class JavaOpenHorseWindowTranslator extends PacketTranslator<ServerOpenHorseWindowPacket> {

    private static final NbtMap ARMOR_SLOT;
    private static final NbtMap CARPET_SLOT;
    private static final NbtMap SADDLE_SLOT;

    static {
        // Build the NBT mappings that Bedrock wants to lay out the GUI
        String[] acceptedHorseArmorIdentifiers = new String[] {"minecraft:horsearmorleather", "minecraft:horsearmoriron",
                "minecraft:horsearmorgold", "minecraft:horsearmordiamond"};
        NbtMapBuilder armorBuilder = NbtMap.builder();
        List<NbtMap> acceptedArmors = new ArrayList<>(4);
        for (String identifier : acceptedHorseArmorIdentifiers) {
            NbtMapBuilder acceptedItemBuilder = NbtMap.builder()
                    .putShort("Aux", Short.MAX_VALUE)
                    .putString("Name", identifier);
            acceptedArmors.add(NbtMap.builder().putCompound("slotItem", acceptedItemBuilder.build()).build());
        }
        armorBuilder.putList("acceptedItems", NbtType.COMPOUND, acceptedArmors);
        NbtMapBuilder armorItem = NbtMap.builder()
                .putShort("Aux", Short.MAX_VALUE)
                .putString("Name", "minecraft:horsearmoriron");
        armorBuilder.putCompound("item", armorItem.build());
        armorBuilder.putInt("slotNumber", 1);
        ARMOR_SLOT = armorBuilder.build();

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

    @Override
    public void translate(ServerOpenHorseWindowPacket packet, GeyserSession session) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (entity == null) {
            return;
        }

        UpdateEquipPacket updateEquipPacket = new UpdateEquipPacket();
        updateEquipPacket.setWindowId((short) packet.getWindowId());
        updateEquipPacket.setWindowType((short) ContainerType.HORSE.getId());
        updateEquipPacket.setUniqueEntityId(entity.getGeyserId());

        NbtMapBuilder builder = NbtMap.builder();
        List<NbtMap> slots = new ArrayList<>();

        InventoryTranslator inventoryTranslator;
        if (entity instanceof LlamaEntity) {
            inventoryTranslator = new LlamaInventoryTranslator(packet.getNumberOfSlots());
            slots.add(CARPET_SLOT);
        } else if (entity instanceof ChestedHorseEntity) {
            inventoryTranslator = new DonkeyInventoryTranslator(packet.getNumberOfSlots());
            slots.add(SADDLE_SLOT);
        } else {
            inventoryTranslator = new HorseInventoryTranslator(packet.getNumberOfSlots());
            slots.add(SADDLE_SLOT);
            slots.add(ARMOR_SLOT);
        }

        // Build the NbtMap that sets the icons for Bedrock (e.g. sets the saddle outline on the saddle slot)
        builder.putList("slots", NbtType.COMPOUND, slots);

        updateEquipPacket.setTag(builder.build());
        session.sendUpstreamPacket(updateEquipPacket);

        session.setInventoryTranslator(inventoryTranslator);
        InventoryUtils.openInventory(session, new Container(entity.getMetadata().getString(EntityData.NAMETAG), packet.getWindowId(), packet.getNumberOfSlots(), null, session.getPlayerInventory()));
    }
}
