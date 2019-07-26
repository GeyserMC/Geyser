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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.nimbusds.jose.JWSObject;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import net.minidev.json.JSONObject;
import org.geysermc.api.events.player.PlayerFormResponseEvent;
import org.geysermc.api.window.CustomFormBuilder;
import org.geysermc.api.window.CustomFormWindow;
import org.geysermc.api.window.FormWindow;
import org.geysermc.api.window.component.InputComponent;
import org.geysermc.api.window.component.LabelComponent;
import org.geysermc.api.window.response.CustomFormResponse;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.BedrockAuthData;
import org.geysermc.connector.network.session.cache.WindowCache;
import org.geysermc.connector.network.translators.Registry;
import org.geysermc.connector.utils.LoginEncryptionUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
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

        JsonNode certData;
        try {
            certData = LoginEncryptionUtils.JSON_MAPPER.readTree(loginPacket.getChainData().toByteArray());
        } catch (IOException ex) {
            throw new RuntimeException("Certificate JSON can not be read.");
        }

        JsonNode certChainData = certData.get("chain");
        if (certChainData.getNodeType() != JsonNodeType.ARRAY) {
            throw new RuntimeException("Certificate data is not valid");
        }

        boolean validChain;
        try {
            validChain = LoginEncryptionUtils.validateChainData(certChainData);

            connector.getLogger().debug(String.format("Is player data valid? %s", validChain));

            JWSObject jwt = JWSObject.parse(certChainData.get(certChainData.size() - 1).asText());
            JsonNode payload = LoginEncryptionUtils.JSON_MAPPER.readTree(jwt.getPayload().toBytes());

            if (payload.get("extraData").getNodeType() != JsonNodeType.OBJECT) {
                throw new RuntimeException("AuthData was not found!");
            }

            JSONObject extraData = (JSONObject) jwt.getPayload().toJSONObject().get("extraData");
            session.setAuthenticationData(new BedrockAuthData(extraData.getAsString("displayName"), UUID.fromString(extraData.getAsString("identity")), extraData.getAsString("XUID")));

            if (payload.get("identityPublicKey").getNodeType() != JsonNodeType.STRING) {
                throw new RuntimeException("Identity Public Key was not found!");
            }

            ECPublicKey identityPublicKey = EncryptionUtils.generateKey(payload.get("identityPublicKey").textValue());
            JWSObject clientJwt = JWSObject.parse(loginPacket.getSkinData().toString());
            EncryptionUtils.verifyJwt(clientJwt, identityPublicKey);

            if (EncryptionUtils.canUseEncryption()) {
                startEncryptionHandshake(identityPublicKey);
            }
        } catch (Exception ex) {
            session.disconnect("disconnectionScreen.internalError.cantConnect");
            throw new RuntimeException("Unable to complete login", ex);
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
                connector.getLogger().info("Player connected with username " + session.getAuthenticationData().getName());
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
        Registry.BEDROCK.translate(packet.getClass(), packet, session);
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
        Registry.BEDROCK.translate(packet.getClass(), packet, session);
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
        WindowCache windowCache = session.getWindowCache();
        if (!windowCache.getWindows().containsKey(packet.getFormId()))
            return false;

        FormWindow window = windowCache.getWindows().remove(packet.getFormId());
        window.setResponse(packet.getFormData().trim());

        if (session.isLoggedIn()) {
            PlayerFormResponseEvent event = new PlayerFormResponseEvent(session, packet.getFormId(), window);
            connector.getPluginManager().runEvent(event);
        } else {
            if (window instanceof CustomFormWindow) {
                CustomFormWindow customFormWindow = (CustomFormWindow) window;
                if (!customFormWindow.getTitle().equals("Login"))
                    return false;

                CustomFormResponse response = (CustomFormResponse) customFormWindow.getResponse();
                session.authenticate(response.getInputResponses().get(2), response.getInputResponses().get(3));

                // Clear windows so authentication data isn't accidentally cached
                windowCache.getWindows().clear();
            }
        }
        return true;
    }

    @Override
    public boolean handle(MoveEntityAbsolutePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        if (!session.isLoggedIn()) {
            CustomFormWindow window = new CustomFormBuilder("Login")
                    .addComponent(new LabelComponent("Minecraft: Java Edition account authentication."))
                    .addComponent(new LabelComponent("Enter the credentials for your Minecraft: Java Edition account below."))
                    .addComponent(new InputComponent("Email/Username", "account@geysermc.org", ""))
                    .addComponent(new InputComponent("Password", "123456", ""))
                    .build();

            session.sendForm(window, 1);
            return true;
        }
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
        Registry.BEDROCK.translate(packet.getClass(), packet, session);
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

    private void startEncryptionHandshake(PublicKey key) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair serverKeyPair = generator.generateKeyPair();

        byte[] token = EncryptionUtils.generateRandomToken();
        SecretKey encryptionKey = EncryptionUtils.getSecretKey(serverKeyPair.getPrivate(), key, token);
        session.getUpstream().enableEncryption(encryptionKey);

        ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
        packet.setJwt(EncryptionUtils.createHandshakeJwt(serverKeyPair, token).serialize());
        session.getUpstream().sendPacketImmediately(packet);
    }
}