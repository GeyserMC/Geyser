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
        /*Registry.registerJava(LoginPluginRequestPacket.class, new JavaLoginPluginMessageTranslator());

        Registry.registerJava(ServerJoinGamePacket.class, new JavaJoinGameTranslator());
        Registry.registerJava(ServerPluginMessagePacket.class, new JavaPluginMessageTranslator());
        Registry.registerJava(ServerChatPacket.class, new JavaChatTranslator());
        Registry.registerJava(ServerTitlePacket.class, new JavaTitleTranslator());
        Registry.registerJava(ServerUpdateTimePacket.class, new JavaUpdateTimeTranslator());
        Registry.registerJava(ServerRespawnPacket.class, new JavaRespawnTranslator());
        Registry.registerJava(ServerSpawnPositionPacket.class, new JavaSpawnPositionTranslator());
        Registry.registerJava(ServerDifficultyPacket.class, new JavaDifficultyTranslator());

        Registry.registerJava(ServerEntityAnimationPacket.class, new JavaEntityAnimationTranslator());
        Registry.registerJava(ServerEntityPositionPacket.class, new JavaEntityPositionTranslator());
        Registry.registerJava(ServerEntityPositionRotationPacket.class, new JavaEntityPositionRotationTranslator());
        Registry.registerJava(ServerEntityTeleportPacket.class, new JavaEntityTeleportTranslator());
        Registry.registerJava(ServerEntityVelocityPacket.class, new JavaEntityVelocityTranslator());
        Registry.registerJava(ServerEntityPropertiesPacket.class, new JavaEntityPropertiesTranslator());
        Registry.registerJava(ServerEntityRotationPacket.class, new JavaEntityRotationTranslator());
        Registry.registerJava(ServerEntityHeadLookPacket.class, new JavaEntityHeadLookTranslator());
        Registry.registerJava(ServerEntityMetadataPacket.class, new JavaEntityMetadataTranslator());
        Registry.registerJava(ServerEntityStatusPacket.class, new JavaEntityStatusTranslator());
        Registry.registerJava(ServerEntityEquipmentPacket.class, new JavaEntityEquipmentTranslator());
        Registry.registerJava(ServerEntityEffectPacket.class, new JavaEntityEffectTranslator());
        Registry.registerJava(ServerEntityRemoveEffectPacket.class, new JavaEntityRemoveEffectTranslator());
        Registry.registerJava(ServerBossBarPacket.class, new JavaBossBarTranslator());

        Registry.registerJava(ServerSpawnExpOrbPacket.class, new JavaSpawnExpOrbTranslator());
        Registry.registerJava(ServerSpawnGlobalEntityPacket.class, new JavaSpawnGlobalEntityTranslator());
        Registry.registerJava(ServerSpawnMobPacket.class, new JavaSpawnMobTranslator());
        Registry.registerJava(ServerSpawnObjectPacket.class, new JavaSpawnObjectTranslator());
        Registry.registerJava(ServerSpawnPaintingPacket.class, new JavaSpawnPaintingTranslator());
        Registry.registerJava(ServerSpawnPlayerPacket.class, new JavaSpawnPlayerTranslator());
        Registry.registerJava(ServerPlayerListEntryPacket.class, new JavaPlayerListEntryTranslator());

        Registry.registerJava(ServerPlayerPositionRotationPacket.class, new JavaPlayerPositionRotationTranslator());
        Registry.registerJava(ServerPlayerSetExperiencePacket.class, new JavaPlayerSetExperienceTranslator());
        Registry.registerJava(ServerPlayerHealthPacket.class, new JavaPlayerHealthTranslator());
        Registry.registerJava(ServerPlayerActionAckPacket.class, new JavaPlayerActionAckTranslator());

        Registry.registerJava(ServerPlayerAbilitiesPacket.class, new JavaPlayerAbilitiesTranslator());

        Registry.registerJava(ServerNotifyClientPacket.class, new JavaNotifyClientTranslator());
        Registry.registerJava(ServerChunkDataPacket.class, new JavaChunkDataTranslator());
        Registry.registerJava(ServerEntityDestroyPacket.class, new JavaEntityDestroyTranslator());
        Registry.registerJava(ServerWindowItemsPacket.class, new JavaWindowItemsTranslator());
        Registry.registerJava(ServerOpenWindowPacket.class, new JavaOpenWindowTranslator());
        Registry.registerJava(ServerSetSlotPacket.class, new JavaSetSlotTranslator());
        Registry.registerJava(ServerScoreboardObjectivePacket.class, new JavaScoreboardObjectiveTranslator());
        Registry.registerJava(ServerDisplayScoreboardPacket.class, new JavaDisplayScoreboardTranslator());
        Registry.registerJava(ServerUpdateScorePacket.class, new JavaUpdateScoreTranslator());
        Registry.registerJava(ServerTeamPacket.class, new JavaTeamTranslator());
        Registry.registerJava(ServerBlockChangePacket.class, new JavaBlockChangeTranslator());
        Registry.registerJava(ServerMultiBlockChangePacket.class, new JavaMultiBlockChangeTranslator());
        Registry.registerJava(ServerUnloadChunkPacket.class, new JavaUnloadChunkTranslator());

        Registry.registerJava(ServerUpdateViewPositionPacket.class, new JavaUpdateViewPositionTranslator());
        Registry.registerJava(ServerUpdateViewDistancePacket.class, new JavaUpdateViewDistanceTranslator());

        Registry.registerJava(ServerOpenWindowPacket.class, new OpenWindowPacketTranslator());

        Registry.registerBedrock(AnimatePacket.class, new BedrockAnimateTranslator());
        Registry.registerBedrock(CommandRequestPacket.class, new BedrockCommandRequestTranslator());
        Registry.registerBedrock(InventoryTransactionPacket.class, new BedrockInventoryTransactionTranslator());
        Registry.registerBedrock(MobEquipmentPacket.class, new BedrockMobEquipmentTranslator());
        Registry.registerBedrock(MovePlayerPacket.class, new BedrockMovePlayerTranslator());
        Registry.registerBedrock(PlayerActionPacket.class, new BedrockActionTranslator());
        Registry.registerBedrock(SetLocalPlayerAsInitializedPacket.class, new BedrockPlayerInitializedTranslator());
        Registry.registerBedrock(InteractPacket.class, new BedrockInteractTranslator());
        Registry.registerBedrock(TextPacket.class, new BedrockTextTranslator());
        Registry.registerBedrock(RespawnPacket.class, new BedrockRespawnTranslator());
        Registry.registerBedrock(ShowCreditsPacket.class, new BedrockShowCreditsTranslator());*/
        
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
