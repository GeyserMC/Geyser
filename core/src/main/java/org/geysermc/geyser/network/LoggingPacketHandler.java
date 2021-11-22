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

package org.geysermc.geyser.network;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;

/**
 * Bare bones implementation of BedrockPacketHandler suitable for extension.
 *
 * Logs and ignores all packets presented. Allows subclasses to override/implement only
 * packets of interest and limit boilerplate code.
 */
public class LoggingPacketHandler implements BedrockPacketHandler {
    protected final GeyserImpl geyser;
    protected final GeyserSession session;

    LoggingPacketHandler(GeyserImpl geyser, GeyserSession session) {
        this.geyser = geyser;
        this.session = session;
    }

    boolean defaultHandler(BedrockPacket packet) {
        geyser.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LoginPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ResourcePackClientResponsePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AdventureSettingsPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AnimatePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(BlockEntityDataPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(BlockPickRequestPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(BookEditPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ClientCacheBlobStatusPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ClientCacheMissResponsePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ClientCacheStatusPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ClientToServerHandshakePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(CommandBlockUpdatePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(CommandRequestPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ContainerClosePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(CraftingEventPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(EntityEventPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(EntityPickRequestPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(EventPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(InteractPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(InventoryContentPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(InventorySlotPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(InventoryTransactionPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ItemFrameDropItemPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(LabTablePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(LecternUpdatePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(LevelEventGenericPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(LevelSoundEventPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(MapInfoRequestPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(MobArmorEquipmentPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(MobEquipmentPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ModalFormResponsePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(MoveEntityAbsolutePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(NetworkStackLatencyPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PhotoTransferPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PlayerActionPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PlayerHotbarPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PlayerInputPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PlayerSkinPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PurchaseReceiptPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(RequestChunkRadiusPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ResourcePackChunkRequestPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(RiderJumpPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ServerSettingsRequestPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetDefaultGameTypePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetLocalPlayerAsInitializedPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetPlayerGameTypePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SubClientLoginPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(TextPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AddBehaviorTreePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AddEntityPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AddHangingEntityPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AddItemEntityPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AddPaintingPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AddPlayerPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AvailableCommandsPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(BlockEventPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(BossEventPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(CameraPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ChangeDimensionPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ChunkRadiusUpdatedPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ClientboundMapItemDataPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(CommandOutputPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ContainerOpenPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ContainerSetDataPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(CraftingDataPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(DisconnectPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ExplodePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(LevelChunkPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(GameRulesChangedPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(GuiDataPickItemPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(HurtArmorPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AutomationClientConnectPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(LevelEventPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(MapCreateLockedCopyPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(MobEffectPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ModalFormRequestPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(MoveEntityDeltaPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(NpcRequestPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(OnScreenTextureAnimationPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PlayerListPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PlaySoundPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PlayStatusPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(RemoveEntityPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(RemoveObjectivePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ResourcePackChunkDataPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ResourcePackDataInfoPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ResourcePacksInfoPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ResourcePackStackPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(RespawnPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ScriptCustomEventPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ServerSettingsResponsePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ServerToClientHandshakePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetCommandsEnabledPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetDifficultyPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetDisplayObjectivePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetEntityDataPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetEntityLinkPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetEntityMotionPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetHealthPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetLastHurtByPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetScoreboardIdentityPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetScorePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetSpawnPositionPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetTimePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SetTitlePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ShowCreditsPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ShowProfilePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ShowStoreOfferPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SimpleEventPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SpawnExperienceOrbPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(StartGamePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(StopSoundPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(StructureBlockUpdatePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(StructureTemplateDataRequestPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(StructureTemplateDataResponsePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(TakeItemEntityPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(TransferPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(UpdateAttributesPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(UpdateBlockPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(UpdateBlockPropertiesPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(UpdateBlockSyncedPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(UpdateEquipPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(UpdateSoftEnumPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(UpdateTradePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AvailableEntityIdentifiersPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(BiomeDefinitionListPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(LevelSoundEvent2Packet packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(NetworkChunkPublisherUpdatePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SpawnParticleEffectPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(VideoStreamConnectPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(EmotePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(TickSyncPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AnvilDamagePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(NetworkSettingsPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PlayerAuthInputPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(SettingsCommandPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(EducationSettingsPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(CompletedUsingItemPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(MultiplayerSettingsPacket packet) {
        return defaultHandler(packet);
    }

    // 1.16 new packets

    @Override
    public boolean handle(DebugInfoPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(EmoteListPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(CodeBuilderPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(CreativeContentPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ItemStackRequestPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(LevelSoundEvent1Packet packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ItemStackResponsePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PlayerArmorDamagePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PlayerEnchantOptionsPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(UpdatePlayerGameTypePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PacketViolationWarningPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PositionTrackingDBClientRequestPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PositionTrackingDBServerBroadcastPacket packet) {
        return defaultHandler(packet);
    }

    // 1.16.100 new packets

    @Override
    public boolean handle(MotionPredictionHintsPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(AnimateEntityPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(CameraShakePacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(PlayerFogPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(CorrectPlayerMovePredictionPacket packet) {
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(ItemComponentPacket packet) {
        return defaultHandler(packet);
    }

    // 1.16.200 new packet

    @Override
    public boolean handle(FilterTextPacket packet) {
        return defaultHandler(packet);
    }
}