/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.network.translators.block.entity.*;
import org.geysermc.connector.network.translators.inventory.*;
import org.geysermc.connector.network.translators.inventory.updater.ContainerInventoryUpdater;
import org.geysermc.connector.network.translators.inventory.updater.InventoryUpdater;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.reflections.Reflections;

import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTOutputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.BedrockPacket;

import lombok.Getter;

public class Translators {

    @Getter
    private static ItemTranslator itemTranslator;

    @Getter
    private static Map<WindowType, InventoryTranslator> inventoryTranslators = new HashMap<>();

    @Getter
    private static Map<String, BlockEntityTranslator> blockEntityTranslators = new HashMap<>();

    private static final CompoundTag EMPTY_TAG = CompoundTagBuilder.builder().buildRootTag();
    public static final byte[] EMPTY_LEVEL_CHUNK_DATA;

    static {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(new byte[258]); // Biomes + Border Size + Extra Data Size

            try (NBTOutputStream stream = NbtUtils.createNetworkWriter(outputStream)) {
                stream.write(EMPTY_TAG);
            }

            EMPTY_LEVEL_CHUNK_DATA = outputStream.toByteArray();
        }catch (IOException e) {
            throw new AssertionError("Unable to generate empty level chunk data");
        }
    }

    @SuppressWarnings("unchecked")
    public static void start() {
        Reflections ref = new Reflections("org.geysermc.connector.network.translators");
        
        for (Class<?> clazz : ref.getTypesAnnotatedWith(Translator.class)) {
            Class<?> packet = clazz.getAnnotation(Translator.class).packet();
            
            GeyserConnector.getInstance().getLogger().debug("Found annotated translator: " + clazz.getCanonicalName() + " : " + packet.getSimpleName());
            
            try {
                if (Packet.class.isAssignableFrom(packet)) {
                    Class<? extends Packet> targetPacket = (Class<? extends Packet>) packet;
                    PacketTranslator<? extends Packet> translator = (PacketTranslator<? extends Packet>) clazz.newInstance();

                    Registry.registerJava(targetPacket, translator);

                } else if (BedrockPacket.class.isAssignableFrom(packet)) {
                    Class<? extends BedrockPacket> targetPacket = (Class<? extends BedrockPacket>) packet;
                    PacketTranslator<? extends BedrockPacket> translator = (PacketTranslator<? extends BedrockPacket>) clazz.newInstance();

                    Registry.registerBedrock(targetPacket, translator);

                } else {
                    GeyserConnector.getInstance().getLogger().error("Class " + clazz.getCanonicalName() + " is annotated as a translator but has an invalid target packet.");
                }
            } catch (InstantiationException | IllegalAccessException e) {
                GeyserConnector.getInstance().getLogger().error("Could not instantiate annotated translator " + clazz.getCanonicalName() + ".");
            }
        }
        
        itemTranslator = new ItemTranslator();
        itemTranslator.init();
        BlockTranslator.init();

        registerBlockEntityTranslators();
        registerInventoryTranslators();
    }

    private static void registerBlockEntityTranslators() {
        Reflections ref = new Reflections("org.geysermc.connector.network.translators.block.entity");

        for (Class<?> clazz : ref.getTypesAnnotatedWith(BlockEntity.class)) {

            GeyserConnector.getInstance().getLogger().debug("Found annotated block entity: " + clazz.getCanonicalName());

            try {
                blockEntityTranslators.put(clazz.getAnnotation(BlockEntity.class).name(), (BlockEntityTranslator) clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                GeyserConnector.getInstance().getLogger().error("Could not instantiate annotated block entity " + clazz.getCanonicalName() + ".");
            }
        }
    }

    private static void registerInventoryTranslators() {
        inventoryTranslators.put(null, new PlayerInventoryTranslator()); //player inventory
        inventoryTranslators.put(WindowType.GENERIC_9X1, new SingleChestInventoryTranslator(9));
        inventoryTranslators.put(WindowType.GENERIC_9X2, new SingleChestInventoryTranslator(18));
        inventoryTranslators.put(WindowType.GENERIC_9X3, new SingleChestInventoryTranslator(27));
        inventoryTranslators.put(WindowType.GENERIC_9X4, new DoubleChestInventoryTranslator(36));
        inventoryTranslators.put(WindowType.GENERIC_9X5, new DoubleChestInventoryTranslator(45));
        inventoryTranslators.put(WindowType.GENERIC_9X6, new DoubleChestInventoryTranslator(54));
        inventoryTranslators.put(WindowType.BREWING_STAND, new BrewingInventoryTranslator());
        inventoryTranslators.put(WindowType.ANVIL, new AnvilInventoryTranslator());
        inventoryTranslators.put(WindowType.CRAFTING, new CraftingInventoryTranslator());
        inventoryTranslators.put(WindowType.GRINDSTONE, new GrindstoneInventoryTranslator());
        //inventoryTranslators.put(WindowType.ENCHANTMENT, new EnchantmentInventoryTranslator()); //TODO

        InventoryTranslator furnace = new FurnaceInventoryTranslator();
        inventoryTranslators.put(WindowType.FURNACE, furnace);
        inventoryTranslators.put(WindowType.BLAST_FURNACE, furnace);
        inventoryTranslators.put(WindowType.SMOKER, furnace);

        InventoryUpdater containerUpdater = new ContainerInventoryUpdater();
        inventoryTranslators.put(WindowType.GENERIC_3X3, new BlockInventoryTranslator(9, "minecraft:dispenser[facing=north,triggered=false]", ContainerType.DISPENSER, containerUpdater));
        inventoryTranslators.put(WindowType.HOPPER, new BlockInventoryTranslator(5, "minecraft:hopper[enabled=false,facing=down]", ContainerType.HOPPER, containerUpdater));
        inventoryTranslators.put(WindowType.SHULKER_BOX, new BlockInventoryTranslator(27, "minecraft:shulker_box[facing=north]", ContainerType.CONTAINER, containerUpdater));
        //inventoryTranslators.put(WindowType.BEACON, new BlockInventoryTranslator(1, "minecraft:beacon", ContainerType.BEACON)); //TODO
    }
}
