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

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.network.translators.inventory.GenericInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
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
    private static InventoryTranslator inventoryTranslator = new GenericInventoryTranslator();

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
        BlockTranslator.init();

        registerInventoryTranslators();
    }

    private static void registerInventoryTranslators() {
        /*inventoryTranslators.put(WindowType.GENERIC_9X1, new GenericInventoryTranslator());
        inventoryTranslators.put(WindowType.GENERIC_9X2, new GenericInventoryTranslator());
        inventoryTranslators.put(WindowType.GENERIC_9X3, new GenericInventoryTranslator());
        inventoryTranslators.put(WindowType.GENERIC_9X4, new GenericInventoryTranslator());
        inventoryTranslators.put(WindowType.GENERIC_9X5, new GenericInventoryTranslator());
        inventoryTranslators.put(WindowType.GENERIC_9X6, new GenericInventoryTranslator());*/
    }
}
