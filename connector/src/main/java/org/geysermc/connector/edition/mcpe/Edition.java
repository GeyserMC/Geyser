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

package org.geysermc.connector.edition.mcpe;

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
import com.github.steveice10.mc.protocol.packet.login.server.LoginDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginPluginRequestPacket;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.AdventureSettingsPacket;
import com.nukkitx.protocol.bedrock.packet.AnimatePacket;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.BlockPickRequestPacket;
import com.nukkitx.protocol.bedrock.packet.CommandRequestPacket;
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket;
import com.nukkitx.protocol.bedrock.packet.EmotePacket;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import com.nukkitx.protocol.bedrock.packet.InteractPacket;
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket;
import com.nukkitx.protocol.bedrock.packet.ItemFrameDropItemPacket;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import com.nukkitx.protocol.bedrock.packet.MapInfoRequestPacket;
import com.nukkitx.protocol.bedrock.packet.MobEquipmentPacket;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import com.nukkitx.protocol.bedrock.packet.PacketViolationWarningPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerActionPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerInputPacket;
import com.nukkitx.protocol.bedrock.packet.PositionTrackingDBClientRequestPacket;
import com.nukkitx.protocol.bedrock.packet.RespawnPacket;
import com.nukkitx.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import com.nukkitx.protocol.bedrock.packet.ShowCreditsPacket;
import com.nukkitx.protocol.bedrock.packet.TextPacket;
import com.nukkitx.protocol.bedrock.v407.Bedrock_v407;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserEdition;
import org.geysermc.connector.entity.AbstractArrowEntity;
import org.geysermc.connector.entity.AreaEffectCloudEntity;
import org.geysermc.connector.entity.BoatEntity;
import org.geysermc.connector.entity.EnderCrystalEntity;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.ExpOrbEntity;
import org.geysermc.connector.entity.FallingBlockEntity;
import org.geysermc.connector.entity.FireworkEntity;
import org.geysermc.connector.entity.FishingHookEntity;
import org.geysermc.connector.entity.FurnaceMinecartEntity;
import org.geysermc.connector.entity.ItemEntity;
import org.geysermc.connector.entity.ItemFrameEntity;
import org.geysermc.connector.entity.ItemedFireballEntity;
import org.geysermc.connector.entity.LeashKnotEntity;
import org.geysermc.connector.entity.MinecartEntity;
import org.geysermc.connector.entity.PaintingEntity;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.entity.SpawnerMinecartEntity;
import org.geysermc.connector.entity.TNTEntity;
import org.geysermc.connector.entity.ThrowableEntity;
import org.geysermc.connector.entity.TippedArrowEntity;
import org.geysermc.connector.entity.TridentEntity;
import org.geysermc.connector.entity.living.AbstractFishEntity;
import org.geysermc.connector.entity.living.AmbientEntity;
import org.geysermc.connector.entity.living.ArmorStandEntity;
import org.geysermc.connector.entity.living.FlyingEntity;
import org.geysermc.connector.entity.living.GolemEntity;
import org.geysermc.connector.entity.living.MagmaCubeEntity;
import org.geysermc.connector.entity.living.SlimeEntity;
import org.geysermc.connector.entity.living.SquidEntity;
import org.geysermc.connector.entity.living.WaterEntity;
import org.geysermc.connector.entity.living.animal.AnimalEntity;
import org.geysermc.connector.entity.living.animal.BeeEntity;
import org.geysermc.connector.entity.living.animal.FoxEntity;
import org.geysermc.connector.entity.living.animal.OcelotEntity;
import org.geysermc.connector.entity.living.animal.PandaEntity;
import org.geysermc.connector.entity.living.animal.PigEntity;
import org.geysermc.connector.entity.living.animal.PolarBearEntity;
import org.geysermc.connector.entity.living.animal.PufferFishEntity;
import org.geysermc.connector.entity.living.animal.RabbitEntity;
import org.geysermc.connector.entity.living.animal.SheepEntity;
import org.geysermc.connector.entity.living.animal.StriderEntity;
import org.geysermc.connector.entity.living.animal.TropicalFishEntity;
import org.geysermc.connector.entity.living.animal.horse.AbstractHorseEntity;
import org.geysermc.connector.entity.living.animal.horse.ChestedHorseEntity;
import org.geysermc.connector.entity.living.animal.horse.HorseEntity;
import org.geysermc.connector.entity.living.animal.horse.LlamaEntity;
import org.geysermc.connector.entity.living.animal.horse.TraderLlamaEntity;
import org.geysermc.connector.entity.living.animal.tameable.CatEntity;
import org.geysermc.connector.entity.living.animal.tameable.ParrotEntity;
import org.geysermc.connector.entity.living.animal.tameable.WolfEntity;
import org.geysermc.connector.entity.living.merchant.AbstractMerchantEntity;
import org.geysermc.connector.entity.living.merchant.VillagerEntity;
import org.geysermc.connector.entity.living.monster.AbstractSkeletonEntity;
import org.geysermc.connector.entity.living.monster.BlazeEntity;
import org.geysermc.connector.entity.living.monster.CreeperEntity;
import org.geysermc.connector.entity.living.monster.ElderGuardianEntity;
import org.geysermc.connector.entity.living.monster.EnderDragonEntity;
import org.geysermc.connector.entity.living.monster.EndermanEntity;
import org.geysermc.connector.entity.living.monster.GiantEntity;
import org.geysermc.connector.entity.living.monster.GuardianEntity;
import org.geysermc.connector.entity.living.monster.MonsterEntity;
import org.geysermc.connector.entity.living.monster.PiglinEntity;
import org.geysermc.connector.entity.living.monster.ShulkerEntity;
import org.geysermc.connector.entity.living.monster.SpiderEntity;
import org.geysermc.connector.entity.living.monster.WitherEntity;
import org.geysermc.connector.entity.living.monster.ZoglinEntity;
import org.geysermc.connector.entity.living.monster.ZombieEntity;
import org.geysermc.connector.entity.living.monster.ZombifiedPiglinEntity;
import org.geysermc.connector.entity.living.monster.raid.AbstractIllagerEntity;
import org.geysermc.connector.entity.living.monster.raid.RaidParticipantEntity;
import org.geysermc.connector.entity.living.monster.raid.SpellcasterIllagerEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.translators.PacketTranslatorRegistry;
import org.geysermc.connector.network.translators.bedrock.BedrockActionTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockAdventureSettingsTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockAnimateTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockBlockEntityDataTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockBlockPickRequestPacketTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockCommandRequestTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockContainerCloseTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockEmoteTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockEntityEventTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockInteractTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockInventoryTransactionTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockItemFrameDropItemTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockLevelSoundEventTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockMapInfoRequestTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockMobEquipmentTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockMoveEntityAbsoluteTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockMovePlayerTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockPacketViolationWarningTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockPlayerInputTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockPositionTrackingDBClientRequestTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockRespawnTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockSetLocalPlayerAsInitializedTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockShowCreditsTranslator;
import org.geysermc.connector.network.translators.bedrock.BedrockTextTranslator;
import org.geysermc.connector.network.translators.inventory.AnvilInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.BlockInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.BrewingInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.CraftingInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.DoubleChestInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.FurnaceInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.GrindstoneInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.inventory.MerchantInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.PlayerInventoryTranslator;
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
import org.geysermc.connector.network.translators.java.JavaLoginDisconnectTranslator;
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
import org.geysermc.connector.network.translators.world.block.entity.BannerBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.BedBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.CampfireBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.DoubleChestBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.EmptyBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.EndGatewayBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.FlowerPotBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.NoteblockBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.PistonBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.ShulkerBoxBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.SignBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.SkullBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.SpawnerBlockEntityTranslator;

public class Edition extends GeyserEdition {

    public Edition(GeyserConnector connector) {
        super(connector, "bedrock");

        // Version
        codec = Bedrock_v407.V407_CODEC;
        pongEdition = "MCPE";

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
//                .blockEntityTranslator(new PistonBlockEntityTranslator()) special case
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
                .bedrockPacketTranslator(AdventureSettingsPacket.class, new BedrockAdventureSettingsTranslator())
                .bedrockPacketTranslator(AnimatePacket.class, new BedrockAnimateTranslator())
                .bedrockPacketTranslator(BlockEntityDataPacket.class, new BedrockBlockEntityDataTranslator())
                .bedrockPacketTranslator(BlockPickRequestPacket.class, new BedrockBlockPickRequestPacketTranslator())
                .bedrockPacketTranslator(CommandRequestPacket.class, new BedrockCommandRequestTranslator())
                .bedrockPacketTranslator(ContainerClosePacket.class, new BedrockContainerCloseTranslator())
                .bedrockPacketTranslator(EmotePacket.class, new BedrockEmoteTranslator())
                .bedrockPacketTranslator(EntityEventPacket.class, new BedrockEntityEventTranslator())
                .bedrockPacketTranslator(InteractPacket.class, new BedrockInteractTranslator())
                .bedrockPacketTranslator(InventoryTransactionPacket.class, new BedrockInventoryTransactionTranslator())
                .bedrockPacketTranslator(ItemFrameDropItemPacket.class, new BedrockItemFrameDropItemTranslator())
                .bedrockPacketTranslator(LevelSoundEventPacket.class, new BedrockLevelSoundEventTranslator())
                .bedrockPacketTranslator(MapInfoRequestPacket.class, new BedrockMapInfoRequestTranslator())
                .bedrockPacketTranslator(MobEquipmentPacket.class, new BedrockMobEquipmentTranslator())
                .bedrockPacketTranslator(MoveEntityAbsolutePacket.class, new BedrockMoveEntityAbsoluteTranslator())
                .bedrockPacketTranslator(MovePlayerPacket.class, new BedrockMovePlayerTranslator())
                .bedrockPacketTranslator(PacketViolationWarningPacket.class, new BedrockPacketViolationWarningTranslator())
                .bedrockPacketTranslator(PlayerInputPacket.class, new BedrockPlayerInputTranslator())
                .bedrockPacketTranslator(PositionTrackingDBClientRequestPacket.class, new BedrockPositionTrackingDBClientRequestTranslator())
                .bedrockPacketTranslator(RespawnPacket.class, new BedrockRespawnTranslator())
                .bedrockPacketTranslator(SetLocalPlayerAsInitializedPacket.class, new BedrockSetLocalPlayerAsInitializedTranslator())
                .bedrockPacketTranslator(ShowCreditsPacket.class, new BedrockShowCreditsTranslator())
                .bedrockPacketTranslator(TextPacket.class, new BedrockTextTranslator());

        // Register Java Packet Translators
        PacketTranslatorRegistry.REGISTER
                .javaPacketTranslator(ServerBossBarPacket.class, new JavaBossBarTranslator())
                .javaPacketTranslator(ServerChatPacket.class, new JavaChatTranslator())
                .javaPacketTranslator(ServerDeclareCommandsPacket.class, new JavaDeclareCommandsTranslator())
                .javaPacketTranslator(ServerDeclareRecipesPacket.class, new JavaDeclareRecipesTranslator())
                .javaPacketTranslator(ServerDifficultyPacket.class, new JavaDifficultyTranslator())
                .javaPacketTranslator(ServerJoinGamePacket.class, new JavaJoinGameTranslator())
                .javaPacketTranslator(LoginDisconnectPacket.class, new JavaLoginDisconnectTranslator())
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

        // Register Inventory Translators
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

        // Register EntityTypes
        EntityType.REGISTER
                .entityType(EntityType.CHICKEN, AnimalEntity.class, 10, 0.7f, 0.4f)
                .entityType(EntityType.COW, AnimalEntity.class, 11, 1.4f, 0.9f)
                .entityType(EntityType.PIG, PigEntity.class, 12, 0.9f)
                .entityType(EntityType.SHEEP, SheepEntity.class, 13, 1.3f, 0.9f)
                .entityType(EntityType.WOLF, WolfEntity.class, 14, 0.85f, 0.6f)
                .entityType(EntityType.VILLAGER, VillagerEntity.class, 15, 1.8f, 0.6f, 0.6f, 1.62f, "minecraft:villager_v2")
                .entityType(EntityType.MOOSHROOM, AnimalEntity.class, 16, 1.4f, 0.9f)
                .entityType(EntityType.SQUID, SquidEntity.class, 17, 0.8f)
                .entityType(EntityType.RABBIT, RabbitEntity.class, 18, 0.5f, 0.4f)
                .entityType(EntityType.BAT, AmbientEntity.class, 19, 0.9f, 0.5f)
                .entityType(EntityType.IRON_GOLEM, GolemEntity.class, 20, 2.7f, 1.4f)
                .entityType(EntityType.SNOW_GOLEM, GolemEntity.class, 21, 1.9f, 0.7f)
                .entityType(EntityType.OCELOT, OcelotEntity.class, 22, 0.35f, 0.3f)
                .entityType(EntityType.HORSE, HorseEntity.class, 23, 1.6f, 1.3965f)
                .entityType(EntityType.DONKEY, ChestedHorseEntity.class, 24, 1.6f, 1.3965f)
                .entityType(EntityType.MULE, ChestedHorseEntity.class, 25, 1.6f, 1.3965f)
                .entityType(EntityType.SKELETON_HORSE, AbstractHorseEntity.class, 26, 1.6f, 1.3965f)
                .entityType(EntityType.ZOMBIE_HORSE, AbstractHorseEntity.class, 27, 1.6f, 1.3965f)
                .entityType(EntityType.POLAR_BEAR, PolarBearEntity.class, 28, 1.4f, 1.3f)
                .entityType(EntityType.LLAMA, LlamaEntity.class, 29, 1.87f, 0.9f)
                .entityType(EntityType.TRADER_LLAMA, TraderLlamaEntity.class, 29, 1.187f, 0.9f, 0f, 0f, "minecraft:llama")
                .entityType(EntityType.PARROT, ParrotEntity.class, 30, 0.9f, 0.5f)
                .entityType(EntityType.DOLPHIN, WaterEntity.class, 31, 0.6f, 0.9f)
                .entityType(EntityType.ZOMBIE, ZombieEntity.class, 32, 1.8f, 0.6f, 0.6f, 1.62f)
                .entityType(EntityType.GIANT, GiantEntity.class, 32, 1.8f, 0.6f, 0.6f, 1.62f, "minecraft:zombie")
                .entityType(EntityType.CREEPER, CreeperEntity.class, 33, 1.7f, 0.6f, 0.6f, 1.62f)
                .entityType(EntityType.SKELETON, AbstractSkeletonEntity.class, 34, 1.8f, 0.6f, 0.6f, 1.62f)
                .entityType(EntityType.SPIDER, SpiderEntity.class, 35, 0.9f, 1.4f, 1.4f, 1f)
                .entityType(EntityType.ZOMBIFIED_PIGLIN, ZombifiedPiglinEntity.class, 36, 1.95f, 0.6f, 0.6f, 1.62f, "minecraft:zombie_pigman")
                .entityType(EntityType.SLIME, SlimeEntity.class, 37, 0.51f)
                .entityType(EntityType.ENDERMAN, EndermanEntity.class, 38, 2.9f, 0.6f)
                .entityType(EntityType.SILVERFISH, MonsterEntity.class, 39, 0.3f, 0.4f)
                .entityType(EntityType.CAVE_SPIDER, MonsterEntity.class, 40, 0.5f, 0.7f)
                .entityType(EntityType.GHAST, FlyingEntity.class, 41, 4.0f)
                .entityType(EntityType.MAGMA_CUBE, MagmaCubeEntity.class, 42, 0.51f)
                .entityType(EntityType.BLAZE, BlazeEntity.class, 43, 1.8f, 0.6f)
                .entityType(EntityType.ZOMBIE_VILLAGER, ZombieEntity.class, 44, 1.8f, 0.6f, 0.6f, 1.62f)
                .entityType(EntityType.WITCH, RaidParticipantEntity.class, 45, 1.8f, 0.6f, 0.6f, 1.62f)
                .entityType(EntityType.STRAY, AbstractSkeletonEntity.class, 46, 1.8f, 0.6f, 0.6f, 1.62f)
                .entityType(EntityType.HUSK, ZombieEntity.class, 47, 1.8f, 0.6f, 0.6f, 1.62f)
                .entityType(EntityType.WITHER_SKELETON, AbstractSkeletonEntity.class, 48, 2.4f, 0.7f)
                .entityType(EntityType.GUARDIAN, GuardianEntity.class, 49, 0.85f)
                .entityType(EntityType.ELDER_GUARDIAN, ElderGuardianEntity.class, 50, 1.9975f)
                .entityType(EntityType.NPC, PlayerEntity.class, 51, 1.8f, 0.6f, 0.6f, 1.62f)
                .entityType(EntityType.WITHER, WitherEntity.class, 52, 3.5f, 0.9f)
                .entityType(EntityType.ENDER_DRAGON, EnderDragonEntity.class, 53, 4f, 13f)
                .entityType(EntityType.SHULKER, ShulkerEntity.class, 54, 1f, 1f)
                .entityType(EntityType.ENDERMITE, MonsterEntity.class, 55, 0.3f, 0.4f)
                .entityType(EntityType.AGENT, Entity.class, 56, 0f)
                .entityType(EntityType.VINDICATOR, AbstractIllagerEntity.class, 57, 1.8f, 0.6f, 0.6f, 1.62f)
                .entityType(EntityType.PILLAGER, AbstractIllagerEntity.class, 114, 1.8f, 0.6f, 0.6f, 1.62f)
                .entityType(EntityType.WANDERING_TRADER, AbstractMerchantEntity.class, 118, 1.8f, 0.6f, 0.6f, 1.62f)
                .entityType(EntityType.PHANTOM, FlyingEntity.class, 58, 0.5f, 0.9f, 0.9f, 0.6f)
                .entityType(EntityType.RAVAGER, RaidParticipantEntity.class, 59, 1.9f, 1.2f)
                .entityType(EntityType.ARMOR_STAND, ArmorStandEntity.class, 61, 1.975f, 0.5f)
                .entityType(EntityType.TRIPOD_CAMERA, Entity.class, 62, 0f)
                .entityType(EntityType.PLAYER, PlayerEntity.class, 63, 1.8f, 0.6f, 0.6f, 1.62f)
                .entityType(EntityType.ITEM, ItemEntity.class, 64, 0.25f, 0.25f)
                .entityType(EntityType.PRIMED_TNT, TNTEntity.class, 65, 0.98f, 0.98f, 0.98f, 0f, "minecraft:tnt")
                .entityType(EntityType.FALLING_BLOCK, FallingBlockEntity.class, 66, 0.98f, 0.98f)
                .entityType(EntityType.MOVING_BLOCK, Entity.class, 67, 0f)
                .entityType(EntityType.THROWN_EXP_BOTTLE, ThrowableEntity.class, 68, 0.25f, 0.25f, 0f, 0f, "minecraft:xp_bottle")
                .entityType(EntityType.EXPERIENCE_ORB, ExpOrbEntity.class, 69, 0f, 0f, 0f, 0f, "minecraft:xp_orb")
                .entityType(EntityType.EYE_OF_ENDER, Entity.class, 70, 0.25f, 0.25f, 0f, 0f, "minecraft:eye_of_ender_signal")
                .entityType(EntityType.END_CRYSTAL, EnderCrystalEntity.class, 71, 2.0f, 2.0f, 2.0f, 0f, "minecraft:ender_crystal")
                .entityType(EntityType.FIREWORK_ROCKET, FireworkEntity.class, 72, 0.25f, 0.25f, 0.25f, 0f, "minecraft:fireworks_rocket")
                .entityType(EntityType.TRIDENT, TridentEntity.class, 73, 0f, 0f, 0f, 0f, "minecraft:thrown_trident")
                .entityType(EntityType.TURTLE, AnimalEntity.class, 74, 0.4f, 1.2f)
                .entityType(EntityType.CAT, CatEntity.class, 75, 0.35f, 0.3f)
                .entityType(EntityType.SHULKER_BULLET, Entity.class, 76, 0.3125f)
                .entityType(EntityType.FISHING_BOBBER, FishingHookEntity.class, 77, 0f, 0f, 0f, 0f, "minecraft:fishing_hook")
                .entityType(EntityType.CHALKBOARD, Entity.class, 78, 0f)
                .entityType(EntityType.DRAGON_FIREBALL, ItemedFireballEntity.class, 79, 1.0f)
                .entityType(EntityType.ARROW, TippedArrowEntity.class, 80, 0.25f, 0.25f)
                .entityType(EntityType.SPECTRAL_ARROW, AbstractArrowEntity.class, 80, 0.25f, 0.25f, 0.25f, 0f, "minecraft:arrow")
                .entityType(EntityType.SNOWBALL, ThrowableEntity.class, 81, 0.25f)
                .entityType(EntityType.THROWN_EGG, ThrowableEntity.class, 82, 0.25f, 0.25f, 0.25f, 0f, "minecraft:egg")
                .entityType(EntityType.PAINTING, PaintingEntity.class, 83, 0f)
                .entityType(EntityType.MINECART, MinecartEntity.class, 84, 0.7f, 0.98f, 0.98f, 0.35f)
                .entityType(EntityType.FIREBALL, ItemedFireballEntity.class, 85, 1.0f)
                .entityType(EntityType.THROWN_POTION, ThrowableEntity.class, 86, 0.25f, 0.25f, 0.25f, 0f, "minecraft:splash_potion")
                .entityType(EntityType.THROWN_ENDERPEARL, ThrowableEntity.class, 87, 0.25f, 0.25f, 0.25f, 0f, "minecraft:ender_pearl")
                .entityType(EntityType.LEASH_KNOT, LeashKnotEntity.class, 88, 0.5f, 0.375f)
                .entityType(EntityType.WITHER_SKULL, Entity.class, 89, 0.3125f)
                .entityType(EntityType.BOAT, BoatEntity.class, 90, 0.7f, 1.6f, 1.6f, 0.35f)
                .entityType(EntityType.WITHER_SKULL_DANGEROUS, Entity.class, 91, 0f)
                .entityType(EntityType.LIGHTNING_BOLT, Entity.class, 93, 0f)
                .entityType(EntityType.SMALL_FIREBALL, ItemedFireballEntity.class, 94, 0.3125f)
                .entityType(EntityType.AREA_EFFECT_CLOUD, AreaEffectCloudEntity.class, 95, 0.5f, 1.0f)
                .entityType(EntityType.MINECART_HOPPER, MinecartEntity.class, 96, 0.7f, 0.98f, 0.98f, 0.35f, "minecraft:hopper_minecart")
                .entityType(EntityType.MINECART_TNT, MinecartEntity.class, 97, 0.7f, 0.98f, 0.98f, 0.35f, "minecraft:tnt_minecart")
                .entityType(EntityType.MINECART_CHEST, MinecartEntity.class, 98, 0.7f, 0.98f, 0.98f, 0.35f, "minecraft:chest_minecart")
                .entityType(EntityType.MINECART_FURNACE, FurnaceMinecartEntity.class, 98, 0.7f, 0.98f, 0.98f, 0.35f, "minecraft:minecart")
                .entityType(EntityType.MINECART_SPAWNER, SpawnerMinecartEntity.class, 98, 0.7f, 0.98f, 0.98f, 0.35f, "minecraft:minecart")
                .entityType(EntityType.MINECART_COMMAND_BLOCK, MinecartEntity.class, 100, 0.7f, 0.98f, 0.98f, 0.35f, "minecraft:command_block_minecart")
                .entityType(EntityType.LINGERING_POTION, ThrowableEntity.class, 101, 0f)
                .entityType(EntityType.LLAMA_SPIT, Entity.class, 102, 0.25f)
                .entityType(EntityType.EVOKER_FANGS, Entity.class, 103, 0.8f, 0.5f, 0.5f, 0f, "minecraft:evocation_fang")
                .entityType(EntityType.EVOKER, SpellcasterIllagerEntity.class, 104, 1.95f, 0.6f, 0.6f, 0f, "minecraft:evocation_illager")
                .entityType(EntityType.VEX, MonsterEntity.class, 105, 0.8f, 0.4f)
                .entityType(EntityType.ICE_BOMB, Entity.class, 106, 0f)
                .entityType(EntityType.BALLOON, Entity.class, 107, 0f) //TODO
                .entityType(EntityType.PUFFERFISH, PufferFishEntity.class, 108, 0.7f, 0.7f)
                .entityType(EntityType.SALMON, AbstractFishEntity.class, 109, 0.5f, 0.7f)
                .entityType(EntityType.DROWNED, ZombieEntity.class, 110, 1.95f, 0.6f)
                .entityType(EntityType.TROPICAL_FISH, TropicalFishEntity.class, 111, 0.6f, 0.6f, 0f, 0f, "minecraft:tropicalfish")
                .entityType(EntityType.COD, AbstractFishEntity.class, 112, 0.25f, 0.5f)
                .entityType(EntityType.PANDA, PandaEntity.class, 113, 1.25f, 1.125f, 1.825f)
                .entityType(EntityType.FOX, FoxEntity.class, 121, 0.5f, 1.25f)
                .entityType(EntityType.ITEM_FRAME, ItemFrameEntity.class, 0, 0,0)
                .entityType(EntityType.ILLUSIONER, AbstractIllagerEntity.class, 114, 1.8f, 0.6f, 0.6f, 1.62f, "minecraft:pillager")
                .entityType(EntityType.BEE, BeeEntity.class, 122, 0.6f, 0.6f)
                .entityType(EntityType.STRIDER, StriderEntity.class, 125, 1.7f, 0.9f, 0f, 0f, "minecraft:strider")
                .entityType(EntityType.HOGLIN, AnimalEntity.class, 124, 1.4f, 1.3965f, 1.3965f, 0f, "minecraft:hoglin")
                .entityType(EntityType.ZOGLIN, ZoglinEntity.class, 126, 1.4f, 1.3965f, 1.3965f, 0f, "minecraft:zoglin")
                .entityType(EntityType.PIGLIN, PiglinEntity.class, 123, 1.95f, 0.6f, 0.6f, 0f, "minecraft:piglin");

    }
}
