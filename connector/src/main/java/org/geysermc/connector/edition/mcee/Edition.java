/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.edition.mcee;

import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerBossBarPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDeclareCommandsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDeclareRecipesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDifficultyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerRespawnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerStopSoundPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerTitlePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityAnimationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityAttachPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityCollectItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEffectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityHeadLookPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPropertiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityRemoveEffectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntitySetPassengersPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityStatusPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityVelocityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerVehicleMovePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerActionAckPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerChangeHeldItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerSetExperiencePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnExpOrbPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnLivingEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPaintingPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerDisplayScoreboardPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerScoreboardObjectivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerUpdateScorePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerCloseWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerTradeListPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowPropertyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockBreakAnimPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockValuePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerExplosionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMapDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerNotifyClientPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayBuiltinSoundPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayEffectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlaySoundPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnParticlePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUnloadChunkPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateLightPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateViewDistancePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateViewPositionPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginPluginRequestPacket;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.AdventureSettingsPacket;
import com.nukkitx.protocol.bedrock.packet.AnimatePacket;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.BlockPickRequestPacket;
import com.nukkitx.protocol.bedrock.packet.CommandRequestPacket;
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import com.nukkitx.protocol.bedrock.packet.InteractPacket;
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket;
import com.nukkitx.protocol.bedrock.packet.ItemFrameDropItemPacket;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import com.nukkitx.protocol.bedrock.packet.MobEquipmentPacket;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerActionPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerInputPacket;
import com.nukkitx.protocol.bedrock.packet.RespawnPacket;
import com.nukkitx.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import com.nukkitx.protocol.bedrock.packet.ShowCreditsPacket;
import com.nukkitx.protocol.bedrock.packet.TextPacket;
import com.nukkitx.protocol.bedrock.v363.Bedrock_v363;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserEdition;
import org.geysermc.connector.edition.mcee.commands.EducationCommand;
import org.geysermc.connector.edition.mcee.network.translators.bedrock.BedrockActionTranslator;
import org.geysermc.connector.edition.mcee.network.translators.bedrock.BedrockRespawnTranslator;
import org.geysermc.connector.edition.mcee.network.translators.inventory.AnvilInventoryTranslator;
import org.geysermc.connector.edition.mcee.network.translators.inventory.CraftingInventoryTranslator;
import org.geysermc.connector.edition.mcee.network.translators.inventory.FurnaceInventoryTranslator;
import org.geysermc.connector.edition.mcee.network.translators.inventory.GrindstoneInventoryTranslator;
import org.geysermc.connector.edition.mcee.network.translators.inventory.MerchantInventoryTranslator;
import org.geysermc.connector.edition.mcee.network.translators.inventory.PlayerInventoryTranslator;
import org.geysermc.connector.edition.mcee.shims.BlockTranslatorShim;
import org.geysermc.connector.edition.mcee.shims.GeyserSessionShim;
import org.geysermc.connector.edition.mcee.shims.LoginEncryptionUtilsShim;
import org.geysermc.connector.edition.mcee.shims.SkinUtilsShim;
import org.geysermc.connector.edition.mcee.utils.TokenManager;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslatorRegistry;
import org.geysermc.connector.network.translators.bedrock.BedrockAdventureSettingsTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockAnimateTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockBlockEntityDataTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockBlockPickRequestPacketTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockCommandRequestTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockContainerCloseTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockEntityEventTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockInteractTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockInventoryTransactionTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockItemFrameDropItemTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockLevelSoundEventTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockMobEquipmentTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockMoveEntityAbsoluteTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockMovePlayerTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockPlayerInputTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockSetLocalPlayerAsInitializedTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockShowCreditsTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockTextTranslator;
import org.geysermc.connector.network.translators.inventory.BlockInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.BrewingInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.DoubleChestInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.inventory.SingleChestInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.updater.ContainerInventoryUpdater;
import org.geysermc.connector.network.translators.inventory.updater.InventoryUpdater;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.network.translators.item.translators.BannerTranslator;
import org.geysermc.connector.network.translators.item.translators.PotionTranslator;
import org.geysermc.connector.network.translators.item.translators.nbt.BasicItemTranslator;
import org.geysermc.connector.network.translators.item.translators.nbt.BookPagesTranslator;
import org.geysermc.connector.network.translators.item.translators.nbt.CrossbowTranslator;
import org.geysermc.connector.network.translators.item.translators.nbt.EnchantedBookTranslator;
import org.geysermc.connector.network.translators.item.translators.nbt.EnchantmentTranslator;
import org.geysermc.connector.network.translators.item.translators.nbt.FireworkTranslator;
import org.geysermc.connector.network.translators.item.translators.nbt.LeatherArmorTranslator;
import org.geysermc.connector.network.translators.item.translators.nbt.MapItemTranslator;
import org.geysermc.connector.network.translators.java.JavaBossBarTranslator;
import org.geysermc.connector.network.translators.java.JavaChatTranslator;
import org.geysermc.connector.network.translators.java.JavaDeclareCommandsTranslator;
import org.geysermc.connector.network.translators.java.JavaDeclareRecipesTranslator;
import org.geysermc.connector.network.translators.java.JavaDifficultyTranslator;
import org.geysermc.connector.network.translators.java.JavaJoinGameTranslator;
import org.geysermc.connector.network.translators.java.JavaLoginPluginMessageTranslator;
import org.geysermc.connector.network.translators.java.JavaPluginMessageTranslator;
import org.geysermc.connector.network.translators.java.JavaRespawnTranslator;
import org.geysermc.connector.network.translators.java.JavaTitleTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityAnimationTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityAttachTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityDestroyTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityEffectTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityEquipmentTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityHeadLookTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityMetadataTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityPositionRotationTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityPositionTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityPropertiesTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityRemoveEffectTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityRotationTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntitySetPassengersTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityStatusTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityTeleportTranslator;
import org.geysermc.connector.network.translators.java.entity.JavaEntityVelocityTranslator;
import org.geysermc.connector.network.translators.java.entity.player.JavaPlayerAbilitiesTranslator;
import org.geysermc.connector.network.translators.java.entity.player.JavaPlayerActionAckTranslator;
import org.geysermc.connector.network.translators.java.entity.player.JavaPlayerChangeHeldItemTranslator;
import org.geysermc.connector.network.translators.java.entity.player.JavaPlayerHealthTranslator;
import org.geysermc.connector.network.translators.java.entity.player.JavaPlayerListEntryTranslator;
import org.geysermc.connector.network.translators.java.entity.player.JavaPlayerPositionRotationTranslator;
import org.geysermc.connector.network.translators.java.entity.player.JavaPlayerSetExperienceTranslator;
import org.geysermc.connector.network.translators.java.entity.player.JavaPlayerStopSoundTranslator;
import org.geysermc.connector.network.translators.java.entity.spawn.JavaSpawnEntityTranslator;
import org.geysermc.connector.network.translators.java.entity.spawn.JavaSpawnExpOrbTranslator;
import org.geysermc.connector.network.translators.java.entity.spawn.JavaSpawnLivingEntityTranslator;
import org.geysermc.connector.network.translators.java.entity.spawn.JavaSpawnPaintingTranslator;
import org.geysermc.connector.network.translators.java.entity.spawn.JavaSpawnPlayerTranslator;
import org.geysermc.connector.network.translators.java.scoreboard.JavaDisplayScoreboardTranslator;
import org.geysermc.connector.network.translators.java.scoreboard.JavaScoreboardObjectiveTranslator;
import org.geysermc.connector.network.translators.java.scoreboard.JavaTeamTranslator;
import org.geysermc.connector.network.translators.java.scoreboard.JavaUpdateScoreTranslator;
import org.geysermc.connector.network.translators.java.window.JavaCloseWindowTranslator;
import org.geysermc.connector.network.translators.java.window.JavaConfirmTransactionTranslator;
import org.geysermc.connector.network.translators.java.window.JavaOpenWindowTranslator;
import org.geysermc.connector.network.translators.java.window.JavaSetSlotTranslator;
import org.geysermc.connector.network.translators.java.window.JavaWindowItemsTranslator;
import org.geysermc.connector.network.translators.java.window.JavaWindowPropertyTranslator;
import org.geysermc.connector.network.translators.java.world.JavaBlockBreakAnimTranslator;
import org.geysermc.connector.network.translators.java.world.JavaBlockChangeTranslator;
import org.geysermc.connector.network.translators.java.world.JavaBlockValueTranslator;
import org.geysermc.connector.network.translators.java.world.JavaChunkDataTranslator;
import org.geysermc.connector.network.translators.java.world.JavaCollectItemTranslator;
import org.geysermc.connector.network.translators.java.world.JavaExplosionTranslator;
import org.geysermc.connector.network.translators.java.world.JavaMapDataTranslator;
import org.geysermc.connector.network.translators.java.world.JavaMultiBlockChangeTranslator;
import org.geysermc.connector.network.translators.java.world.JavaNotifyClientTranslator;
import org.geysermc.connector.network.translators.java.world.JavaPlayBuiltinSoundTranslator;
import org.geysermc.connector.network.translators.java.world.JavaPlayEffectTranslator;
import org.geysermc.connector.network.translators.java.world.JavaPlayerPlaySoundTranslator;
import org.geysermc.connector.network.translators.java.world.JavaSpawnParticleTranslator;
import org.geysermc.connector.network.translators.java.world.JavaSpawnPositionTranslator;
import org.geysermc.connector.network.translators.java.world.JavaTradeListTranslator;
import org.geysermc.connector.network.translators.java.world.JavaUnloadChunkTranslator;
import org.geysermc.connector.network.translators.java.world.JavaUpdateTileEntityTranslator;
import org.geysermc.connector.network.translators.java.world.JavaUpdateTimeTranslator;
import org.geysermc.connector.network.translators.java.world.JavaUpdateViewDistanceTranslator;
import org.geysermc.connector.network.translators.java.world.JavaUpdateViewPositionTranslator;
import org.geysermc.connector.network.translators.java.world.JavaVehicleMoveTranslator;
import org.geysermc.connector.network.translators.sound.SoundHandlerRegistry;
import org.geysermc.connector.network.translators.sound.block.BucketSoundInteractionHandler;
import org.geysermc.connector.network.translators.sound.block.ComparatorSoundInteractHandler;
import org.geysermc.connector.network.translators.sound.block.DoorSoundInteractionHandler;
import org.geysermc.connector.network.translators.sound.block.FlintAndSteelInteractionHandler;
import org.geysermc.connector.network.translators.sound.block.GrassPathInteractionHandler;
import org.geysermc.connector.network.translators.sound.block.HoeInteractionHandler;
import org.geysermc.connector.network.translators.sound.block.LeverSoundInteractionHandler;
import org.geysermc.connector.network.translators.sound.entity.MilkCowSoundInteractionHandler;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.world.block.entity.BannerBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.BedBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.CampfireBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.DoubleChestBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.EmptyBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.EndGatewayBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.FlowerPotBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.NoteblockBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.ShulkerBoxBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.SignBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.SkullBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.SpawnerBlockEntityTranslator;
import org.geysermc.connector.utils.LoginEncryptionUtils;
import org.geysermc.connector.utils.SkinUtils;

public class Edition extends GeyserEdition {

    @Getter
    private final TokenManager tokenManager;

    public Edition(GeyserConnector connector) {
        super(connector, "education");

        tokenManager = new TokenManager(this);

        // Version
        codec = Bedrock_v363.V363_CODEC;
        pongEdition = "MCEE";

        // Register Block Entity Translators
        BlockEntityTranslator.REGISTER
                .blockEntityTranslator(new BannerBlockEntityTranslator())
                .blockEntityTranslator(new BedBlockEntityTranslator())
                .blockEntityTranslator(new CampfireBlockEntityTranslator())
                .blockEntityTranslator(new DoubleChestBlockEntityTranslator())
                .blockEntityTranslator(new EmptyBlockEntityTranslator())
                .blockEntityTranslator(new EndGatewayBlockEntityTranslator())
                .blockEntityTranslator(new FlowerPotBlockEntityTranslator())
                .blockEntityTranslator(new NoteblockBlockEntityTranslator())
                .blockEntityTranslator(new ShulkerBoxBlockEntityTranslator())
                .blockEntityTranslator(new SignBlockEntityTranslator())
                .blockEntityTranslator(new SkullBlockEntityTranslator())
                .blockEntityTranslator(new SpawnerBlockEntityTranslator());

        // Register Ignored Packets
        PacketTranslatorRegistry.REGISTER
                .ignoredPackets(ServerKeepAlivePacket.class)
                .ignoredPackets(ServerPlayerListDataPacket.class)
                .ignoredPackets(ServerUpdateLightPacket.class);

        // Register Bedrock Packet Translators
        PacketTranslatorRegistry.REGISTER
                .bedrockPacketTranslator(PlayerActionPacket.class, new BedrockActionTranslator())
                .bedrockPacketTranslator(AnimatePacket.class, new BedrockAnimateTranslator())
                .bedrockPacketTranslator(BlockEntityDataPacket.class, new BedrockBlockEntityDataTranslator())
                .bedrockPacketTranslator(BlockPickRequestPacket.class, new BedrockBlockPickRequestPacketTranslator())
                .bedrockPacketTranslator(CommandRequestPacket.class, new BedrockCommandRequestTranslator())
                .bedrockPacketTranslator(ContainerClosePacket.class, new BedrockContainerCloseTranslator())
                .bedrockPacketTranslator(EntityEventPacket.class, new BedrockEntityEventTranslator())
                .bedrockPacketTranslator(InteractPacket.class, new BedrockInteractTranslator())
                .bedrockPacketTranslator(InventoryTransactionPacket.class, new BedrockInventoryTransactionTranslator())
                .bedrockPacketTranslator(ItemFrameDropItemPacket.class, new BedrockItemFrameDropItemTranslator())
                .bedrockPacketTranslator(LevelSoundEventPacket.class, new BedrockLevelSoundEventTranslator())
                .bedrockPacketTranslator(MobEquipmentPacket.class, new BedrockMobEquipmentTranslator())
                .bedrockPacketTranslator(MoveEntityAbsolutePacket.class, new BedrockMoveEntityAbsoluteTranslator())
                .bedrockPacketTranslator(MovePlayerPacket.class, new BedrockMovePlayerTranslator())
                .bedrockPacketTranslator(PlayerInputPacket.class, new BedrockPlayerInputTranslator())
                .bedrockPacketTranslator(RespawnPacket.class, new BedrockRespawnTranslator())
                .bedrockPacketTranslator(SetLocalPlayerAsInitializedPacket.class, new BedrockSetLocalPlayerAsInitializedTranslator())
                .bedrockPacketTranslator(ShowCreditsPacket.class, new BedrockShowCreditsTranslator())
                .bedrockPacketTranslator(TextPacket.class, new BedrockTextTranslator())
                .bedrockPacketTranslator(AdventureSettingsPacket.class, new BedrockAdventureSettingsTranslator());

        // Register Java Packet Translators
        PacketTranslatorRegistry.REGISTER
                .javaPacketTranslator(ServerBossBarPacket.class, new JavaBossBarTranslator())
                .javaPacketTranslator(ServerChatPacket.class, new JavaChatTranslator())
                .javaPacketTranslator(ServerDeclareCommandsPacket.class, new JavaDeclareCommandsTranslator())
                .javaPacketTranslator(ServerDeclareRecipesPacket.class, new JavaDeclareRecipesTranslator())
                .javaPacketTranslator(ServerDifficultyPacket.class, new JavaDifficultyTranslator())
                .javaPacketTranslator(ServerJoinGamePacket.class, new JavaJoinGameTranslator())
                .javaPacketTranslator(LoginPluginRequestPacket.class, new JavaLoginPluginMessageTranslator())
                .javaPacketTranslator(ServerPluginMessagePacket.class, new JavaPluginMessageTranslator())
                .javaPacketTranslator(ServerRespawnPacket.class, new JavaRespawnTranslator())
                .javaPacketTranslator(ServerTitlePacket.class, new JavaTitleTranslator());

        // Register Java Entity Packet Translators
        PacketTranslatorRegistry.REGISTER
                .javaPacketTranslator(ServerEntityAnimationPacket.class, new JavaEntityAnimationTranslator())
                .javaPacketTranslator(ServerEntityAttachPacket.class, new JavaEntityAttachTranslator())
                .javaPacketTranslator(ServerEntityDestroyPacket.class, new JavaEntityDestroyTranslator())
                .javaPacketTranslator(ServerEntityEffectPacket.class, new JavaEntityEffectTranslator())
                .javaPacketTranslator(ServerEntityEquipmentPacket.class, new JavaEntityEquipmentTranslator())
                .javaPacketTranslator(ServerEntityHeadLookPacket.class, new JavaEntityHeadLookTranslator())
                .javaPacketTranslator(ServerEntityMetadataPacket.class, new JavaEntityMetadataTranslator())
                .javaPacketTranslator(ServerEntityPositionRotationPacket.class, new JavaEntityPositionRotationTranslator())
                .javaPacketTranslator(ServerEntityPositionPacket.class, new JavaEntityPositionTranslator())
                .javaPacketTranslator(ServerEntityPropertiesPacket.class, new JavaEntityPropertiesTranslator())
                .javaPacketTranslator(ServerEntityRemoveEffectPacket.class, new JavaEntityRemoveEffectTranslator())
                .javaPacketTranslator(ServerEntityRotationPacket.class, new JavaEntityRotationTranslator())
                .javaPacketTranslator(ServerEntitySetPassengersPacket.class, new JavaEntitySetPassengersTranslator())
                .javaPacketTranslator(ServerEntityStatusPacket.class, new JavaEntityStatusTranslator())
                .javaPacketTranslator(ServerEntityTeleportPacket.class, new JavaEntityTeleportTranslator())
                .javaPacketTranslator(ServerEntityVelocityPacket.class, new JavaEntityVelocityTranslator());

        // Register Java Entity Player Packet Translators
        PacketTranslatorRegistry.REGISTER
                .javaPacketTranslator(ServerPlayerAbilitiesPacket.class, new JavaPlayerAbilitiesTranslator())
                .javaPacketTranslator(ServerPlayerActionAckPacket.class, new JavaPlayerActionAckTranslator())
                .javaPacketTranslator(ServerPlayerChangeHeldItemPacket.class, new JavaPlayerChangeHeldItemTranslator())
                .javaPacketTranslator(ServerPlayerHealthPacket.class, new JavaPlayerHealthTranslator())
                .javaPacketTranslator(ServerPlayerListEntryPacket.class, new JavaPlayerListEntryTranslator())
                .javaPacketTranslator(ServerPlayerPositionRotationPacket.class, new JavaPlayerPositionRotationTranslator())
                .javaPacketTranslator(ServerPlayerSetExperiencePacket.class, new JavaPlayerSetExperienceTranslator())
                .javaPacketTranslator(ServerStopSoundPacket.class, new JavaPlayerStopSoundTranslator());

        // Register Java Entity Spawn Packet Translators
        PacketTranslatorRegistry.REGISTER
                .javaPacketTranslator(ServerSpawnEntityPacket.class, new JavaSpawnEntityTranslator())
                .javaPacketTranslator(ServerSpawnExpOrbPacket.class, new JavaSpawnExpOrbTranslator())
                .javaPacketTranslator(ServerSpawnLivingEntityPacket.class, new JavaSpawnLivingEntityTranslator())
                .javaPacketTranslator(ServerSpawnPaintingPacket.class, new JavaSpawnPaintingTranslator())
                .javaPacketTranslator(ServerSpawnPlayerPacket.class, new JavaSpawnPlayerTranslator());

        // Register Java Scoreboard Packet Translators
        PacketTranslatorRegistry.REGISTER
                .javaPacketTranslator(ServerDisplayScoreboardPacket.class, new JavaDisplayScoreboardTranslator())
                .javaPacketTranslator(ServerScoreboardObjectivePacket.class, new JavaScoreboardObjectiveTranslator())
                .javaPacketTranslator(ServerTeamPacket.class, new JavaTeamTranslator())
                .javaPacketTranslator(ServerUpdateScorePacket.class, new JavaUpdateScoreTranslator());

        // Register Java Window Packet Translators
        PacketTranslatorRegistry.REGISTER
                .javaPacketTranslator(ServerCloseWindowPacket.class, new JavaCloseWindowTranslator())
                .javaPacketTranslator(ServerConfirmTransactionPacket.class, new JavaConfirmTransactionTranslator())
                .javaPacketTranslator(ServerOpenWindowPacket.class, new JavaOpenWindowTranslator())
                .javaPacketTranslator(ServerSetSlotPacket.class, new JavaSetSlotTranslator())
                .javaPacketTranslator(ServerWindowItemsPacket.class, new JavaWindowItemsTranslator())
                .javaPacketTranslator(ServerWindowPropertyPacket.class, new JavaWindowPropertyTranslator());

        // Register Java World Packet Translators
        PacketTranslatorRegistry.REGISTER
                .javaPacketTranslator(ServerBlockBreakAnimPacket.class, new JavaBlockBreakAnimTranslator())
                .javaPacketTranslator(ServerBlockChangePacket.class, new JavaBlockChangeTranslator())
                .javaPacketTranslator(ServerBlockValuePacket.class, new JavaBlockValueTranslator())
                .javaPacketTranslator(ServerChunkDataPacket.class, new JavaChunkDataTranslator())
                .javaPacketTranslator(ServerEntityCollectItemPacket.class, new JavaCollectItemTranslator())
                .javaPacketTranslator(ServerExplosionPacket.class, new JavaExplosionTranslator())
                .javaPacketTranslator(ServerMapDataPacket.class, new JavaMapDataTranslator())
                .javaPacketTranslator(ServerMultiBlockChangePacket.class, new JavaMultiBlockChangeTranslator())
                .javaPacketTranslator(ServerNotifyClientPacket.class, new JavaNotifyClientTranslator())
                .javaPacketTranslator(ServerPlayBuiltinSoundPacket.class, new JavaPlayBuiltinSoundTranslator())
                .javaPacketTranslator(ServerPlayEffectPacket.class, new JavaPlayEffectTranslator())
                .javaPacketTranslator(ServerPlaySoundPacket.class, new JavaPlayerPlaySoundTranslator())
                .javaPacketTranslator(ServerSpawnParticlePacket.class, new JavaSpawnParticleTranslator())
                .javaPacketTranslator(ServerSpawnPositionPacket.class, new JavaSpawnPositionTranslator())
                .javaPacketTranslator(ServerTradeListPacket.class, new JavaTradeListTranslator())
                .javaPacketTranslator(ServerUnloadChunkPacket.class, new JavaUnloadChunkTranslator())
                .javaPacketTranslator(ServerUpdateTileEntityPacket.class, new JavaUpdateTileEntityTranslator())
                .javaPacketTranslator(ServerUpdateTimePacket.class, new JavaUpdateTimeTranslator())
                .javaPacketTranslator(ServerUpdateViewDistancePacket.class, new JavaUpdateViewDistanceTranslator())
                .javaPacketTranslator(ServerUpdateViewPositionPacket.class, new JavaUpdateViewPositionTranslator())
                .javaPacketTranslator(ServerVehicleMovePacket.class, new JavaVehicleMoveTranslator());

        InventoryTranslator.REGISTER
                .inventoryTranslator(null, new PlayerInventoryTranslator())
                .inventoryTranslator(WindowType.GENERIC_9X1, new SingleChestInventoryTranslator(9))
                .inventoryTranslator(WindowType.GENERIC_9X2, new SingleChestInventoryTranslator(18))
                .inventoryTranslator(WindowType.GENERIC_9X3, new SingleChestInventoryTranslator(27))
                .inventoryTranslator(WindowType.GENERIC_9X4, new DoubleChestInventoryTranslator(36))
                .inventoryTranslator(WindowType.GENERIC_9X5, new DoubleChestInventoryTranslator(45))
                .inventoryTranslator(WindowType.GENERIC_9X6, new DoubleChestInventoryTranslator(54))
                .inventoryTranslator(WindowType.BREWING_STAND, new BrewingInventoryTranslator())
                .inventoryTranslator(WindowType.ANVIL, new AnvilInventoryTranslator())
                .inventoryTranslator(WindowType.CRAFTING, new CraftingInventoryTranslator())
                .inventoryTranslator(WindowType.GRINDSTONE, new GrindstoneInventoryTranslator())
                .inventoryTranslator(WindowType.MERCHANT, new MerchantInventoryTranslator());
//                .inventoryTranslator(WindowType.ENCHANTMENT, new EnchantmentInventoryTranslator()); //@TODO

        // Register Inventory Furnace Translators
        InventoryTranslator furnace = new FurnaceInventoryTranslator();

        InventoryTranslator.REGISTER
                .inventoryTranslator(WindowType.FURNACE, furnace)
                .inventoryTranslator(WindowType.BLAST_FURNACE, furnace)
                .inventoryTranslator(WindowType.SMOKER, furnace);

        // Register Inventory Container Translators
        InventoryUpdater containerUpdater = new ContainerInventoryUpdater();

        InventoryTranslator.REGISTER
                .inventoryTranslator(WindowType.GENERIC_3X3, new BlockInventoryTranslator(9, "minecraft:dispenser[facing=north,triggered=false]", ContainerType.DISPENSER, containerUpdater))
                .inventoryTranslator(WindowType.HOPPER, new BlockInventoryTranslator(5, "minecraft:hopper[enabled=false,facing=down]", ContainerType.HOPPER, containerUpdater))
                .inventoryTranslator(WindowType.SHULKER_BOX, new BlockInventoryTranslator(27, "minecraft:shulker_box[facing=north]", ContainerType.CONTAINER, containerUpdater));
//                .inventoryTranslator(WindowType.BEACON, new BlockInventoryTranslator(1, "minecraft:beacon", ContainerType.BEACON)) //@TODO

        // Register Item Translators
        ItemTranslator.REGISTER
                .itemTranslator(new BannerTranslator())
                .itemTranslator(new PotionTranslator());

        // Register Item NBT Translators
        ItemTranslator.REGISTER
                .nbtItemStackTranslator(new BasicItemTranslator())
                .nbtItemStackTranslator(new BookPagesTranslator())
                .nbtItemStackTranslator(new CrossbowTranslator())
                .nbtItemStackTranslator(new EnchantedBookTranslator())
                .nbtItemStackTranslator(new EnchantmentTranslator())
                .nbtItemStackTranslator(new FireworkTranslator())
                .nbtItemStackTranslator(new LeatherArmorTranslator())
                .nbtItemStackTranslator(new MapItemTranslator());

        // Register Block Sound Handlers
        SoundHandlerRegistry.REGISTER
                .soundInteractionHandler(new BucketSoundInteractionHandler())
                .soundInteractionHandler(new ComparatorSoundInteractHandler())
                .soundInteractionHandler(new DoorSoundInteractionHandler())
                .soundInteractionHandler(new FlintAndSteelInteractionHandler())
                .soundInteractionHandler(new GrassPathInteractionHandler())
                .soundInteractionHandler(new HoeInteractionHandler())
                .soundInteractionHandler(new LeverSoundInteractionHandler());

        // Register Entity Sound Handlers
        SoundHandlerRegistry.REGISTER
                .soundInteractionHandler(new MilkCowSoundInteractionHandler());

        // Register Shims
        SkinUtils.REGISTER
                .shim(new SkinUtilsShim());

        // Token Manager
        LoginEncryptionUtils.REGISTER
                .shim(new LoginEncryptionUtilsShim(tokenManager));

        BlockTranslator.REGISTER
                .shim(new BlockTranslatorShim());

        // Geyser Session
        GeyserSession.REGISTER
                .shim(new GeyserSessionShim());

        // Register Commands (we wait till its not null)
        new Thread(() -> {
            for(int count = 0; count < 10; count++) {
                if (connector.getBootstrap().getGeyserCommandManager() != null) {
                    connector.getBootstrap().getGeyserCommandManager().registerCommand(new EducationCommand(connector, "education", "Education Commands", "geyser.command.education", tokenManager));
                    break;
                }
                try {
                    Thread.sleep(500*count);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();


    }
}
