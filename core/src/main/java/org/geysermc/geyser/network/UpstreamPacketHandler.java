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
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.data.ExperimentData;
import com.nukkitx.protocol.bedrock.data.ResourcePackType;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.v471.Bedrock_v471;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.auth.AuthType;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.pack.ResourcePack;
import org.geysermc.geyser.pack.ResourcePackManifest;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.*;

import java.io.FileInputStream;
import java.io.InputStream;

public class UpstreamPacketHandler extends LoggingPacketHandler {

    public UpstreamPacketHandler(GeyserImpl geyser, GeyserSession session) {
        super(geyser, session);
    }

    private boolean translateAndDefault(BedrockPacket packet) {
        return Registries.BEDROCK_PACKET_TRANSLATORS.translate(packet.getClass(), packet, session);
    }

    @Override
    boolean defaultHandler(BedrockPacket packet) {
        return translateAndDefault(packet);
    }

    @Override
    public boolean handle(LoginPacket loginPacket) {
        if (geyser.isShuttingDown()) {
            // Don't allow new players in if we're no longer operating
            session.disconnect(GeyserLocale.getLocaleStringLog("geyser.core.shutdown.kick.message"));
            return true;
        }

        BedrockPacketCodec packetCodec = MinecraftProtocol.getBedrockCodec(loginPacket.getProtocolVersion());
        if (packetCodec == null) {
            String supportedVersions = MinecraftProtocol.getAllSupportedVersions();
            if (loginPacket.getProtocolVersion() > MinecraftProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion()) {
                // Too early to determine session locale
                session.getGeyser().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.outdated.server", supportedVersions));
                session.disconnect(GeyserLocale.getLocaleStringLog("geyser.network.outdated.server", supportedVersions));
                return true;
            } else if (loginPacket.getProtocolVersion() < MinecraftProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion()) {
                session.getGeyser().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.outdated.client", supportedVersions));
                session.disconnect(GeyserLocale.getLocaleStringLog("geyser.network.outdated.client", supportedVersions));
                return true;
            }
        }

        session.getUpstream().getSession().setPacketCodec(packetCodec);

        // Set the block translation based off of version
        session.setBlockMappings(BlockRegistries.BLOCKS.forVersion(loginPacket.getProtocolVersion()));
        session.setItemMappings(Registries.ITEMS.forVersion(loginPacket.getProtocolVersion()));

        LoginEncryptionUtils.encryptPlayerConnection(session, loginPacket);

        PlayStatusPacket playStatus = new PlayStatusPacket();
        playStatus.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        session.sendUpstreamPacket(playStatus);

        geyser.getSessionManager().addPendingSession(session);

        ResourcePacksInfoPacket resourcePacksInfo = new ResourcePacksInfoPacket();
        for(ResourcePack resourcePack : ResourcePack.PACKS.values()) {
            ResourcePackManifest.Header header = resourcePack.getManifest().getHeader();
            resourcePacksInfo.getResourcePackInfos().add(new ResourcePacksInfoPacket.Entry(
                    header.getUuid().toString(), header.getVersionString(), resourcePack.getFile().length(),
                            "", "", "", false, false));
        }
        resourcePacksInfo.setForcedToAccept(GeyserImpl.getInstance().getConfig().isForceResourcePacks());
        session.sendUpstreamPacket(resourcePacksInfo);

        GeyserLocale.loadGeyserLocale(session.getLocale());
        return true;
    }

    @Override
    public boolean handle(ResourcePackClientResponsePacket packet) {
        switch (packet.getStatus()) {
            case COMPLETED:
                if (geyser.getConfig().getRemote().getAuthType() != AuthType.ONLINE) {
                    session.authenticate(session.getAuthData().name());
                } else if (!couldLoginUserByName(session.getAuthData().name())) {
                    // We must spawn the white world
                    session.connect();
                }
                geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.connect", session.getAuthData().name()));
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

                if (session.getItemMappings().getFurnaceMinecartData() != null) {
                    // Allow custom items to work
                    stackPacket.getExperiments().add(new ExperimentData("data_driven_items", true));
                }

                if (session.getUpstream().getProtocolVersion() <= Bedrock_v471.V471_CODEC.getProtocolVersion()) {
                    // Allow extended world height in the overworld to work for pre-1.18 clients
                    stackPacket.getExperiments().add(new ExperimentData("caves_and_cliffs", true));
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
        session.executeInEventLoop(() -> session.getFormCache().handleResponse(packet));
        return true;
    }

    private boolean couldLoginUserByName(String bedrockUsername) {
        if (geyser.getConfig().getUserAuths() != null) {
            GeyserConfiguration.IUserAuthenticationInfo info = geyser.getConfig().getUserAuths().get(bedrockUsername);

            if (info != null) {
                geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.auth.stored_credentials", session.getAuthData().name()));
                session.setMicrosoftAccount(info.isMicrosoftAccount());
                session.authenticate(info.getEmail(), info.getPassword());
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        if (session.isLoggingIn()) {
            SetTitlePacket titlePacket = new SetTitlePacket();
            titlePacket.setType(SetTitlePacket.Type.ACTIONBAR);
            titlePacket.setText(GeyserLocale.getPlayerLocaleString("geyser.auth.login.wait", session.getLocale()));
            titlePacket.setFadeInTime(0);
            titlePacket.setFadeOutTime(1);
            titlePacket.setStayTime(2);
            titlePacket.setXuid("");
            titlePacket.setPlatformOnlineId("");
            session.sendUpstreamPacket(titlePacket);
        }

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
