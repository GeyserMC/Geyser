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

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.nimbusds.jose.JWSObject;
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
        // TODO: Implement support for multiple protocols
        if (loginPacket.getProtocolVersion() != GeyserConnector.BEDROCK_PACKET_CODEC.getProtocolVersion()) {
            connector.getLogger().debug("unsupported");
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
        connector.getLogger().debug("Handled " + textPacket.getClass().getSimpleName());
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
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AnimatePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        switch (packet.getAction()) {
            case SWING_ARM:
                ClientPlayerSwingArmPacket swingArmPacket = new ClientPlayerSwingArmPacket(Hand.MAIN_HAND);
                session.getDownstream().getSession().send(swingArmPacket);
        }
        return true;
    }

    @Override
    public boolean handle(BlockEntityDataPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(BlockPickRequestPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(BookEditPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ClientCacheBlobStatusPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ClientCacheMissResponsePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ClientCacheStatusPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ClientToServerHandshakePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(CommandBlockUpdatePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(CommandRequestPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());

        String command = packet.getCommand().replace("/", "");
        if (connector.getCommandMap().getCommands().containsKey(command)) {
            connector.getCommandMap().runCommand(session, command);
        } else {
            ClientChatPacket chatPacket = new ClientChatPacket(packet.getCommand());
            session.getDownstream().getSession().send(chatPacket);
        }

        return true;
    }

    @Override
    public boolean handle(ContainerClosePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(CraftingEventPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(EntityEventPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(EntityFallPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(EntityPickRequestPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(EventPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(InteractPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(InventoryContentPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(InventorySlotPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(InventoryTransactionPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ItemFrameDropItemPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LabTablePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LecternUpdatePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LevelEventGenericPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LevelSoundEventPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LevelSoundEvent3Packet packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MapInfoRequestPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MobArmorEquipmentPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MobEquipmentPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ModalFormResponsePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MoveEntityAbsolutePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(NetworkStackLatencyPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PhotoTransferPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlayerActionPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlayerHotbarPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlayerInputPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlayerSkinPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PurchaseReceiptPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(RequestChunkRadiusPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ResourcePackChunkRequestPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(RiderJumpPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ServerSettingsRequestPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetDefaultGameTypePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetLocalPlayerAsInitializedPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetPlayerGameTypePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SubClientLoginPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(TextPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());

        if (packet.getMessage().charAt(0) == '.') {
            ClientChatPacket chatPacket = new ClientChatPacket(packet.getMessage().replace(".", "/"));
            session.getDownstream().getSession().send(chatPacket);
            return true;
        }

        ClientChatPacket chatPacket = new ClientChatPacket(packet.getMessage());
        session.getDownstream().getSession().send(chatPacket);

        return true;
    }

    @Override
    public boolean handle(AddBehaviorTreePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AddEntityPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AddHangingEntityPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AddItemEntityPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AddPaintingPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AddPlayerPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AvailableCommandsPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(BlockEventPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(BossEventPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(CameraPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ChangeDimensionPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ChunkRadiusUpdatedPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ClientboundMapItemDataPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(CommandOutputPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ContainerOpenPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ContainerSetDataPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(CraftingDataPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(DisconnectPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ExplodePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LevelChunkPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(GameRulesChangedPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(GuiDataPickItemPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(HurtArmorPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AutomationClientConnectPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LevelEventPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MapCreateLockedCopyPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MobEffectPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ModalFormRequestPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MoveEntityDeltaPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(NpcRequestPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(OnScreenTextureAnimationPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlayerListPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlaySoundPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(PlayStatusPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(RemoveEntityPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(RemoveObjectivePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ResourcePackChunkDataPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ResourcePackDataInfoPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ResourcePacksInfoPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ResourcePackStackPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(RespawnPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ScriptCustomEventPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ServerSettingsResponsePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ServerToClientHandshakePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetCommandsEnabledPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetDifficultyPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetDisplayObjectivePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetEntityDataPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetEntityLinkPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetEntityMotionPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetHealthPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetLastHurtByPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetScoreboardIdentityPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetScorePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetSpawnPositionPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetTimePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SetTitlePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ShowCreditsPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ShowProfilePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(ShowStoreOfferPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SimpleEventPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SpawnExperienceOrbPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(StartGamePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(StopSoundPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(StructureBlockUpdatePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(StructureTemplateDataExportRequestPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(StructureTemplateDataExportResponsePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(TakeItemEntityPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(TransferPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateAttributesPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateBlockPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateBlockPropertiesPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateBlockSyncedPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateEquipPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateSoftEnumPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(UpdateTradePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(AvailableEntityIdentifiersPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(BiomeDefinitionListPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(LevelSoundEvent2Packet packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(NetworkChunkPublisherUpdatePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(SpawnParticleEffectPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(VideoStreamConnectPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }
}