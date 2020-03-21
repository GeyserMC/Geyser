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

import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.github.steveice10.mc.protocol.packet.ingame.server.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerDisplayScoreboardPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerScoreboardObjectivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerUpdateScorePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.*;
import com.github.steveice10.mc.protocol.packet.login.server.LoginPluginRequestPacket;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTOutputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.packet.*;
import lombok.Getter;
import org.geysermc.connector.network.translators.bedrock.*;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.network.translators.inventory.*;
import org.geysermc.connector.network.translators.inventory.updater.ContainerInventoryUpdater;
import org.geysermc.connector.network.translators.inventory.updater.InventoryUpdater;
import org.geysermc.connector.network.translators.block.entity.*;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.network.translators.java.*;
import org.geysermc.connector.network.translators.java.entity.*;
import org.geysermc.connector.network.translators.java.entity.player.*;
import org.geysermc.connector.network.translators.java.entity.spawn.*;
import org.geysermc.connector.network.translators.java.scoreboard.JavaDisplayScoreboardTranslator;
import org.geysermc.connector.network.translators.java.scoreboard.JavaScoreboardObjectiveTranslator;
import org.geysermc.connector.network.translators.java.scoreboard.JavaTeamTranslator;
import org.geysermc.connector.network.translators.java.scoreboard.JavaUpdateScoreTranslator;
import org.geysermc.connector.network.translators.java.window.*;
import org.geysermc.connector.network.translators.java.world.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TranslatorsInit {

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
        } catch (IOException e) {
            throw new AssertionError("Unable to generate empty level chunk data");
        }
    }

    public static void start() {
        Registry.registerJava(LoginPluginRequestPacket.class, new JavaLoginPluginMessageTranslator());

        Registry.registerJava(ServerJoinGamePacket.class, new JavaJoinGameTranslator());
        Registry.registerJava(ServerPluginMessagePacket.class, new JavaPluginMessageTranslator());
        Registry.registerJava(ServerChatPacket.class, new JavaChatTranslator());
        Registry.registerJava(ServerTitlePacket.class, new JavaTitleTranslator());
        Registry.registerJava(ServerUpdateTimePacket.class, new JavaUpdateTimeTranslator());
        Registry.registerJava(ServerRespawnPacket.class, new JavaRespawnTranslator());
        Registry.registerJava(ServerSpawnPositionPacket.class, new JavaSpawnPositionTranslator());
        Registry.registerJava(ServerDifficultyPacket.class, new JavaDifficultyTranslator());
        Registry.registerJava(ServerDeclareRecipesPacket.class, new JavaDeclareRecipesTranslator());

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
        Registry.registerJava(ServerSpawnParticlePacket.class, new JavaSpawnParticlePacket());

        Registry.registerJava(ServerPlayerPositionRotationPacket.class, new JavaPlayerPositionRotationTranslator());
        Registry.registerJava(ServerPlayerSetExperiencePacket.class, new JavaPlayerSetExperienceTranslator());
        Registry.registerJava(ServerPlayerHealthPacket.class, new JavaPlayerHealthTranslator());
        Registry.registerJava(ServerPlayerActionAckPacket.class, new JavaPlayerActionAckTranslator());
        Registry.registerJava(ServerPlayerChangeHeldItemPacket.class, new JavaPlayerChangeHeldItemTranslator());

        Registry.registerJava(ServerPlayerAbilitiesPacket.class, new JavaPlayerAbilitiesTranslator());

        Registry.registerJava(ServerNotifyClientPacket.class, new JavaNotifyClientTranslator());
        Registry.registerJava(ServerChunkDataPacket.class, new JavaChunkDataTranslator());
        Registry.registerJava(ServerEntityDestroyPacket.class, new JavaEntityDestroyTranslator());
        Registry.registerJava(ServerScoreboardObjectivePacket.class, new JavaScoreboardObjectiveTranslator());
        Registry.registerJava(ServerDisplayScoreboardPacket.class, new JavaDisplayScoreboardTranslator());
        Registry.registerJava(ServerUpdateScorePacket.class, new JavaUpdateScoreTranslator());
        Registry.registerJava(ServerTeamPacket.class, new JavaTeamTranslator());
        Registry.registerJava(ServerBlockChangePacket.class, new JavaBlockChangeTranslator());
        Registry.registerJava(ServerMultiBlockChangePacket.class, new JavaMultiBlockChangeTranslator());
        Registry.registerJava(ServerUnloadChunkPacket.class, new JavaUnloadChunkTranslator());
        Registry.registerJava(ServerUpdateTileEntityPacket.class, new JavaUpdateTileEntityTranslator());
        Registry.registerJava(ServerBlockValuePacket.class, new JavaBlockValueTranslator());

        Registry.registerJava(ServerWindowItemsPacket.class, new JavaWindowItemsTranslator());
        Registry.registerJava(ServerOpenWindowPacket.class, new JavaOpenWindowTranslator());
        Registry.registerJava(ServerSetSlotPacket.class, new JavaSetSlotTranslator());
        Registry.registerJava(ServerCloseWindowPacket.class, new JavaCloseWindowTranslator());
        Registry.registerJava(ServerConfirmTransactionPacket.class, new JavaConfirmTransactionTranslator());
        Registry.registerJava(ServerWindowPropertyPacket.class, new JavaWindowPropertyTranslator());

        Registry.registerJava(ServerUpdateViewPositionPacket.class, new JavaUpdateViewPositionTranslator());
        Registry.registerJava(ServerUpdateViewDistancePacket.class, new JavaUpdateViewDistanceTranslator());

        Registry.registerBedrock(AnimatePacket.class, new BedrockAnimateTranslator());
        Registry.registerBedrock(CommandRequestPacket.class, new BedrockCommandRequestTranslator());
        Registry.registerBedrock(InventoryTransactionPacket.class, new BedrockInventoryTransactionTranslator());
        Registry.registerBedrock(MobEquipmentPacket.class, new BedrockMobEquipmentTranslator());
        Registry.registerBedrock(MovePlayerPacket.class, new BedrockMovePlayerTranslator());
        Registry.registerBedrock(PlayerActionPacket.class, new BedrockActionTranslator());
        Registry.registerBedrock(SetLocalPlayerAsInitializedPacket.class, new BedrockPlayerInitializedTranslator());
        Registry.registerBedrock(InteractPacket.class, new BedrockInteractTranslator());
        Registry.registerBedrock(TextPacket.class, new BedrockTextTranslator());
        Registry.registerBedrock(ContainerClosePacket.class, new BedrockContainerCloseTranslator());
        Registry.registerBedrock(RespawnPacket.class, new BedrockRespawnTranslator());
        Registry.registerBedrock(ShowCreditsPacket.class, new BedrockShowCreditsTranslator());

        itemTranslator = new ItemTranslator();
        BlockTranslator.init();

        registerBlockEntityTranslators();
        registerInventoryTranslators();
    }

    private static void registerBlockEntityTranslators() {
        blockEntityTranslators.put("Empty", new EmptyBlockEntityTranslator());
        blockEntityTranslators.put("Sign", new SignBlockEntityTranslator());
        blockEntityTranslators.put("Campfire", new CampfireBlockEntityTranslator());
        blockEntityTranslators.put("Banner", new BannerBlockEntityTranslator());
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
