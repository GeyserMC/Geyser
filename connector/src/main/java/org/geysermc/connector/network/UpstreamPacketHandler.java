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

package org.geysermc.connector.network;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.data.ExperimentData;
import com.nukkitx.protocol.bedrock.data.ResourcePackType;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.v428.Bedrock_v428;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.common.AuthType;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.AdvancementsCache;
import org.geysermc.connector.network.translators.PacketTranslatorRegistry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.world.block.BlockTranslator1_16_100;
import org.geysermc.connector.network.translators.world.block.BlockTranslator1_16_210;
import org.geysermc.connector.utils.*;

import java.io.FileInputStream;
import java.io.InputStream;

public class UpstreamPacketHandler extends LoggingPacketHandler {

    public UpstreamPacketHandler(GeyserConnector connector, GeyserSession session) {
        super(connector, session);
    }

    private boolean translateAndDefault(BedrockPacket packet) {
        return PacketTranslatorRegistry.BEDROCK_TRANSLATOR.translate(packet.getClass(), packet, session);
    }

    @Override
    public boolean handle(LoginPacket loginPacket) {
        BedrockPacketCodec packetCodec = BedrockProtocol.getBedrockCodec(loginPacket.getProtocolVersion());
        if (packetCodec == null) {
            if (loginPacket.getProtocolVersion() > BedrockProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion()) {
                // Too early to determine session locale
                session.getConnector().getLogger().info(LanguageUtils.getLocaleStringLog("geyser.network.outdated.server", BedrockProtocol.DEFAULT_BEDROCK_CODEC.getMinecraftVersion()));
                session.disconnect(LanguageUtils.getLocaleStringLog("geyser.network.outdated.server", BedrockProtocol.DEFAULT_BEDROCK_CODEC.getMinecraftVersion()));
                return true;
            } else if (loginPacket.getProtocolVersion() < BedrockProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion()) {
                session.getConnector().getLogger().info(LanguageUtils.getLocaleStringLog("geyser.network.outdated.client", BedrockProtocol.DEFAULT_BEDROCK_CODEC.getMinecraftVersion()));
                session.disconnect(LanguageUtils.getLocaleStringLog("geyser.network.outdated.client", BedrockProtocol.DEFAULT_BEDROCK_CODEC.getMinecraftVersion()));
                return true;
            }
        }

        session.getUpstream().getSession().setPacketCodec(packetCodec);

        // Set the block translation based off of version
        session.setBlockTranslator(packetCodec.getProtocolVersion() >= Bedrock_v428.V428_CODEC.getProtocolVersion()
                ? BlockTranslator1_16_210.INSTANCE : BlockTranslator1_16_100.INSTANCE);

        LoginEncryptionUtils.encryptPlayerConnection(connector, session, loginPacket);

        PlayStatusPacket playStatus = new PlayStatusPacket();
        playStatus.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        session.sendUpstreamPacket(playStatus);

        ResourcePacksInfoPacket resourcePacksInfo = new ResourcePacksInfoPacket();
        for(ResourcePack resourcePack : ResourcePack.PACKS.values()) {
            ResourcePackManifest.Header header = resourcePack.getManifest().getHeader();
            resourcePacksInfo.getResourcePackInfos().add(new ResourcePacksInfoPacket.Entry(
                    header.getUuid().toString(), header.getVersionString(), resourcePack.getFile().length(),
                            "", "", "", false, false));
        }
        resourcePacksInfo.setForcedToAccept(GeyserConnector.getInstance().getConfig().isForceResourcePacks());
        session.sendUpstreamPacket(resourcePacksInfo);
        return true;
    }

    @Override
    public boolean handle(ResourcePackClientResponsePacket packet) {
        switch (packet.getStatus()) {
            case COMPLETED:
                session.connect();
                connector.getLogger().info(LanguageUtils.getLocaleStringLog("geyser.network.connect", session.getAuthData().getName()));
                break;

            case SEND_PACKS:
                for(String id : packet.getPackIds()) {
                    ResourcePackDataInfoPacket data = new ResourcePackDataInfoPacket();
                    String[] packID = id.split("_");
                    ResourcePack pack = ResourcePack.PACKS.get(packID[0]);
                    ResourcePackManifest.Header header = pack.getManifest().getHeader();

                    data.setPackId(header.getUuid());
                    int chunkCount = (int) Math.ceil((int) pack.getFile().length() / (double) ResourcePack.CHUNK_SIZE);
                    data.setChunkCount(chunkCount);
                    data.setCompressedPackSize(pack.getFile().length());
                    data.setMaxChunkSize(ResourcePack.CHUNK_SIZE);
                    data.setHash(pack.getSha256());
                    data.setPackVersion(packID[1]);
                    data.setPremium(false);
                    data.setType(ResourcePackType.RESOURCE);

                    session.sendUpstreamPacket(data);
                }
                break;

            case HAVE_ALL_PACKS:
                ResourcePackStackPacket stackPacket = new ResourcePackStackPacket();
                stackPacket.setExperimentsPreviouslyToggled(false);
                stackPacket.setForcedToAccept(false); // Leaving this as false allows the player to choose to download or not
                stackPacket.setGameVersion(session.getClientData().getGameVersion());

                for (ResourcePack pack : ResourcePack.PACKS.values()) {
                    ResourcePackManifest.Header header = pack.getManifest().getHeader();
                    stackPacket.getResourcePacks().add(new ResourcePackStackPacket.Entry(header.getUuid().toString(), header.getVersionString(), ""));
                }

                if (ItemRegistry.FURNACE_MINECART_DATA != null) {
                    // Allow custom items to work
                    stackPacket.getExperiments().add(new ExperimentData("data_driven_items", true));
                }

                session.sendUpstreamPacket(stackPacket);
                break;

            default:
                session.disconnect("disconnectionScreen.resourcePack");
                break;
        }

        return true;
    }

    @Override
    public boolean handle(ModalFormResponsePacket packet) {
        switch (packet.getFormId()) {
            case AdvancementsCache.ADVANCEMENT_INFO_FORM_ID:
                return session.getAdvancementsCache().handleInfoForm(packet.getFormData());
            case AdvancementsCache.ADVANCEMENTS_LIST_FORM_ID:
                return session.getAdvancementsCache().handleListForm(packet.getFormData());
            case AdvancementsCache.ADVANCEMENTS_MENU_FORM_ID:
                return session.getAdvancementsCache().handleMenuForm(packet.getFormData());
            case SettingsUtils.SETTINGS_FORM_ID:
                return SettingsUtils.handleSettingsForm(session, packet.getFormData());
            case StatisticsUtils.STATISTICS_LIST_FORM_ID:
                return StatisticsUtils.handleListForm(session, packet.getFormData());
            case StatisticsUtils.STATISTICS_MENU_FORM_ID:
                return StatisticsUtils.handleMenuForm(session, packet.getFormData());
        }

        return LoginEncryptionUtils.authenticateFromForm(session, connector, packet.getFormId(), packet.getFormData());
    }

    private boolean couldLoginUserByName(String bedrockUsername) {
        if (connector.getConfig().getUserAuths() != null) {
            GeyserConfiguration.IUserAuthenticationInfo info = connector.getConfig().getUserAuths().get(bedrockUsername);

            if (info != null) {
                connector.getLogger().info(LanguageUtils.getLocaleStringLog("geyser.auth.stored_credentials", session.getAuthData().getName()));
                session.setMicrosoftAccount(info.isMicrosoftAccount());
                session.authenticate(info.getEmail(), info.getPassword());

                // TODO send a message to bedrock user telling them they are connected (if nothing like a motd
                //      somes from the Java server w/in a few seconds)
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean handle(SetLocalPlayerAsInitializedPacket packet) {
        LanguageUtils.loadGeyserLocale(session.getLocale());

        if (!session.isLoggedIn() && !session.isLoggingIn() && session.getRemoteAuthType() == AuthType.ONLINE) {
            // TODO it is safer to key authentication on something that won't change (UUID, not username)
            if (!couldLoginUserByName(session.getAuthData().getName())) {
                LoginEncryptionUtils.showLoginWindow(session);
            }
            // else we were able to log the user in
        }
        return translateAndDefault(packet);
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        if (session.isLoggingIn()) {
            SetTitlePacket titlePacket = new SetTitlePacket();
            titlePacket.setType(SetTitlePacket.Type.ACTIONBAR);
            titlePacket.setText(LanguageUtils.getPlayerLocaleString("geyser.auth.login.wait", session.getLocale()));
            titlePacket.setFadeInTime(0);
            titlePacket.setFadeOutTime(1);
            titlePacket.setStayTime(2);
            session.sendUpstreamPacket(titlePacket);
        }

        return translateAndDefault(packet);
    }

    @Override
    boolean defaultHandler(BedrockPacket packet) {
        return translateAndDefault(packet);
    }

    @Override
    public boolean handle(ResourcePackChunkRequestPacket packet) {
        ResourcePackChunkDataPacket data = new ResourcePackChunkDataPacket();
        ResourcePack pack = ResourcePack.PACKS.get(packet.getPackId().toString());

        data.setChunkIndex(packet.getChunkIndex());
        data.setProgress(packet.getChunkIndex() * ResourcePack.CHUNK_SIZE);
        data.setPackVersion(packet.getPackVersion());
        data.setPackId(packet.getPackId());

        int offset = packet.getChunkIndex() * ResourcePack.CHUNK_SIZE;
        byte[] packData = new byte[(int) MathUtils.constrain(pack.getFile().length() - offset, 0, ResourcePack.CHUNK_SIZE)];

        try (InputStream inputStream = new FileInputStream(pack.getFile())) {
            inputStream.skip(offset);
            inputStream.read(packData, 0, packData.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

        data.setData(packData);

        session.sendUpstreamPacket(data);
        return true;
    }
}
