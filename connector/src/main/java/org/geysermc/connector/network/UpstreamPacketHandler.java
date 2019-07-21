/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.nimbusds.jose.JWSObject;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.UUID;

public class UpstreamPacketHandler implements BedrockPacketHandler {

    private GeyserConnector connector;
    private GeyserSession session;

    public UpstreamPacketHandler(GeyserConnector connector, GeyserSession session) {
        this.connector = connector;
        this.session = session;
    }

    @Override
    public boolean handle(LoginPacket loginPacket) {
        System.err.println("Handled " + loginPacket.getClass().getSimpleName());
        // TODO: Implement support for multiple protocols
        if (loginPacket.getProtocolVersion() != GeyserConnector.BEDROCK_PACKET_CODEC.getProtocolVersion()) {
            System.out.println("unsupported");
            session.getUpstream().disconnect("Unsupported Bedrock version. Are you running an outdated version?");
            return true;
        }

        session.getUpstream().setPacketCodec(GeyserConnector.BEDROCK_PACKET_CODEC);

        try {
            JSONObject chainData = (JSONObject) JSONValue.parse(loginPacket.getChainData().array());
            JSONArray chainArray = (JSONArray) chainData.get("chain");

            Object identityObject = chainArray.get(chainArray.size() - 1);

            JWSObject identity = JWSObject.parse((String) identityObject);
            JSONObject extraData = (JSONObject) identity.getPayload().toJSONObject().get("extraData");

            session.setAuthenticationData(extraData.getAsString("displayName"), UUID.fromString(extraData.getAsString("identity")), extraData.getAsString("XUID"));
        } catch (Exception ex) {
            session.getUpstream().disconnect("An internal error occurred when connecting to this server.");
            ex.printStackTrace();
            return true;
        }



        PlayStatusPacket playStatus = new PlayStatusPacket();

        playStatus.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);

        session.getUpstream().sendPacketImmediately(playStatus);

        ResourcePacksInfoPacket resourcePacksInfo = new ResourcePacksInfoPacket();
        session.getUpstream().sendPacketImmediately(resourcePacksInfo);
        return true;
    }

    @Override
    public boolean handle(ResourcePackClientResponsePacket textPacket) {
        System.err.println("Handled " + textPacket.getClass().getSimpleName());
        switch (textPacket.getStatus()) {
            case COMPLETED:
                session.connect(connector.getRemoteServer());
                connector.getLogger().info("Player connected with " + session.getAuthenticationData().getName());
                break;
            case HAVE_ALL_PACKS:
                ResourcePackStackPacket stack = new ResourcePackStackPacket();
                stack.setExperimental(false);
                stack.setForcedToAccept(false);
                session.getUpstream().sendPacketImmediately(stack);
                break;
            default:
                session.getUpstream().disconnect("disconnectionScreen.resourcePack");
                break;
        }

        return true;
    }

    @Override
    public boolean handle(AdventureSettingsPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AnimatePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(BlockEntityDataPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(BlockPickRequestPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(BookEditPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ClientCacheBlobStatusPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ClientCacheMissResponsePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ClientCacheStatusPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ClientToServerHandshakePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(CommandBlockUpdatePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(CommandRequestPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ContainerClosePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(CraftingEventPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(EntityEventPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(EntityFallPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(EntityPickRequestPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(EventPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(InteractPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(InventoryContentPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(InventorySlotPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(InventoryTransactionPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ItemFrameDropItemPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LabTablePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LecternUpdatePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LevelEventGenericPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LevelSoundEventPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LevelSoundEvent3Packet packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MapInfoRequestPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MobArmorEquipmentPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MobEquipmentPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ModalFormResponsePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MoveEntityAbsolutePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(NetworkStackLatencyPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PhotoTransferPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlayerActionPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlayerHotbarPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlayerInputPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlayerSkinPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PurchaseReceiptPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(RequestChunkRadiusPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ResourcePackChunkRequestPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(RiderJumpPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ServerSettingsRequestPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetDefaultGameTypePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetLocalPlayerAsInitializedPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetPlayerGameTypePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SubClientLoginPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(TextPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());

        ClientChatPacket chatPacket = new ClientChatPacket(packet.getMessage());
        session.getDownstream().getSession().send(chatPacket);

        return true;
    }

    @Override
    public boolean handle(AddBehaviorTreePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AddEntityPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AddHangingEntityPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AddItemEntityPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AddPaintingPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AddPlayerPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AvailableCommandsPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(BlockEventPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(BossEventPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(CameraPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ChangeDimensionPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ChunkRadiusUpdatedPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ClientboundMapItemDataPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(CommandOutputPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ContainerOpenPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ContainerSetDataPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(CraftingDataPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(DisconnectPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ExplodePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LevelChunkPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(GameRulesChangedPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(GuiDataPickItemPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(HurtArmorPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AutomationClientConnectPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LevelEventPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MapCreateLockedCopyPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MobEffectPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ModalFormRequestPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MoveEntityDeltaPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(NpcRequestPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(OnScreenTextureAnimationPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlayerListPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlaySoundPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlayStatusPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(RemoveEntityPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(RemoveObjectivePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ResourcePackChunkDataPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ResourcePackDataInfoPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ResourcePacksInfoPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ResourcePackStackPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(RespawnPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ScriptCustomEventPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ServerSettingsResponsePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ServerToClientHandshakePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetCommandsEnabledPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetDifficultyPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetDisplayObjectivePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetEntityDataPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetEntityLinkPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetEntityMotionPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetHealthPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetLastHurtByPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetScoreboardIdentityPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetScorePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetSpawnPositionPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetTimePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetTitlePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ShowCreditsPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ShowProfilePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ShowStoreOfferPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SimpleEventPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SpawnExperienceOrbPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(StartGamePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(StopSoundPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(StructureBlockUpdatePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(StructureTemplateDataExportRequestPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(StructureTemplateDataExportResponsePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(TakeItemEntityPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(TransferPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateAttributesPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateBlockPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateBlockPropertiesPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateBlockSyncedPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateEquipPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateSoftEnumPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateTradePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AvailableEntityIdentifiersPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(BiomeDefinitionListPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LevelSoundEvent2Packet packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(NetworkChunkPublisherUpdatePacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SpawnParticleEffectPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(VideoStreamConnectPacket packet) {
        System.out.println("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }
}