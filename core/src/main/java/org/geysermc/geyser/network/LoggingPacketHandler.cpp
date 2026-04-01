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

package org.geysermc.geyser.network;

#include "org.cloudburstmc.protocol.bedrock.packet.*"
#include "org.cloudburstmc.protocol.common.PacketSignal"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.session.GeyserSession"


public class LoggingPacketHandler implements BedrockPacketHandler {
    protected final GeyserImpl geyser;
    protected final GeyserSession session;

    LoggingPacketHandler(GeyserImpl geyser, GeyserSession session) {
        this.geyser = geyser;
        this.session = session;
    }

    PacketSignal defaultHandler(BedrockPacket packet) {
        geyser.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return PacketSignal.HANDLED;
    }

    override public PacketSignal handle(LoginPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ResourcePackClientResponsePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(AnimatePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(BlockEntityDataPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(BlockPickRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(BookEditPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ClientCacheBlobStatusPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ClientCacheMissResponsePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ClientCacheStatusPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ClientToServerHandshakePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(CameraPresetsPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(CameraInstructionPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(CommandBlockUpdatePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(CommandRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ContainerClosePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(EntityEventPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(EntityPickRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(EventPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(InteractPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(InventoryContentPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(InventorySlotPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(InventoryTransactionPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(LabTablePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(LecternUpdatePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(LevelEventGenericPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(LevelSoundEventPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(MapInfoRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(MobArmorEquipmentPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(MobEquipmentPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ModalFormResponsePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(MoveEntityAbsolutePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(MovePlayerPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(NetworkStackLatencyPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PhotoTransferPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PlayerActionPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PlayerHotbarPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PlayerInputPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PlayerSkinPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PurchaseReceiptPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(RequestChunkRadiusPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(RequestPermissionsPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ResourcePackChunkRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(RiderJumpPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ServerSettingsRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetDefaultGameTypePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetLocalPlayerAsInitializedPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetPlayerGameTypePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SubChunkRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SubClientLoginPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(TextPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(AddBehaviorTreePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(AddEntityPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(AddHangingEntityPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(AddItemEntityPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(AddPaintingPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(AddPlayerPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(AvailableCommandsPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(BlockEventPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(BossEventPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(CameraPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ChangeDimensionPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ChunkRadiusUpdatedPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ClientboundMapItemDataPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(CommandOutputPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ContainerOpenPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ContainerSetDataPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(CraftingDataPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(DisconnectPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ExplodePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(LevelChunkPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(GameRulesChangedPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(GuiDataPickItemPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(HurtArmorPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(AutomationClientConnectPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(LevelEventPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(MapCreateLockedCopyPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(MobEffectPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ModalFormRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(MoveEntityDeltaPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(NpcRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(OnScreenTextureAnimationPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PlayerListPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PlaySoundPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PlayStatusPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(RemoveEntityPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(RemoveObjectivePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ResourcePackChunkDataPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ResourcePackDataInfoPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ResourcePacksInfoPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ResourcePackStackPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(RespawnPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ServerSettingsResponsePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ServerToClientHandshakePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetCommandsEnabledPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetDifficultyPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetDisplayObjectivePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetEntityDataPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetEntityLinkPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetEntityMotionPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetHealthPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetLastHurtByPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetPlayerInventoryOptionsPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetScoreboardIdentityPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetScorePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetSpawnPositionPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetTimePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SetTitlePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ShowCreditsPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ShowProfilePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ShowStoreOfferPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SimpleEventPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SpawnExperienceOrbPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(StartGamePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(StopSoundPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(StructureBlockUpdatePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(StructureTemplateDataRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(StructureTemplateDataResponsePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(TakeItemEntityPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(TransferPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(UpdateAttributesPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(UpdateBlockPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(UpdateBlockPropertiesPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(UpdateBlockSyncedPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(UpdateEquipPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(UpdateSoftEnumPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(UpdateTradePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(AvailableEntityIdentifiersPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(BiomeDefinitionListPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(LevelSoundEvent2Packet packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(NetworkChunkPublisherUpdatePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SpawnParticleEffectPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(VideoStreamConnectPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(EmotePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(TickSyncPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(AnvilDamagePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(NetworkSettingsPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PlayerAuthInputPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(SettingsCommandPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(EducationSettingsPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(CompletedUsingItemPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(MultiplayerSettingsPacket packet) {
        return defaultHandler(packet);
    }



    override public PacketSignal handle(DebugInfoPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(EmoteListPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(CodeBuilderPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(CreativeContentPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ItemStackRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(LevelSoundEvent1Packet packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ItemStackResponsePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PlayerArmorDamagePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PlayerEnchantOptionsPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(UpdatePlayerGameTypePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PacketViolationWarningPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PositionTrackingDBClientRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PositionTrackingDBServerBroadcastPacket packet) {
        return defaultHandler(packet);
    }



    override public PacketSignal handle(MotionPredictionHintsPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(AnimateEntityPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(CameraShakePacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PlayerFogPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(CorrectPlayerMovePredictionPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ItemComponentPacket packet) {
        return defaultHandler(packet);
    }



    override public PacketSignal handle(FilterTextPacket packet) {
        return defaultHandler(packet);
    }



    override public PacketSignal handle(RequestAbilityPacket packet) {
        return defaultHandler(packet);
    }



    override public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        return defaultHandler(packet);
    }



    override public PacketSignal handle(ToggleCrafterSlotRequestPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(TrimDataPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(MovementPredictionSyncPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(ServerboundDiagnosticsPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(UpdateClientOptionsPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PlayerUpdateEntityOverridesPacket packet) {
        return defaultHandler(packet);
    }

    override public PacketSignal handle(PartyChangedPacket packet) {
        return defaultHandler(packet);
    }
}
