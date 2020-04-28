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

package org.geysermc.connector.network;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.common.AuthType;
import org.geysermc.common.IGeyserConfiguration;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.Registry;
import org.geysermc.connector.utils.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

public class UpstreamPacketHandler extends LoggingPacketHandler {

    public UpstreamPacketHandler(GeyserConnector connector, GeyserSession session) {
        super(connector, session);
    }

    private boolean translateAndDefault(BedrockPacket packet) {
        return Registry.BEDROCK.translate(packet.getClass(), packet, session);
    }

    @Override
    public boolean handle(LoginPacket loginPacket) {
        if (loginPacket.getProtocolVersion() > GeyserConnector.BEDROCK_PACKET_CODEC.getProtocolVersion()) {
            session.disconnect("Outdated Geyser proxy! I'm still on " + GeyserConnector.BEDROCK_PACKET_CODEC.getMinecraftVersion());
            return true;
        } else if (loginPacket.getProtocolVersion() < GeyserConnector.BEDROCK_PACKET_CODEC.getProtocolVersion()) {
            session.disconnect("Outdated Bedrock client! Please use " + GeyserConnector.BEDROCK_PACKET_CODEC.getMinecraftVersion());
            return true;
        }

        LoginEncryptionUtils.encryptPlayerConnection(connector, session, loginPacket);

        PlayStatusPacket playStatus = new PlayStatusPacket();
        playStatus.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        session.getUpstream().sendPacket(playStatus);

        ResourcePacksInfoPacket resourcePacksInfo = new ResourcePacksInfoPacket();
        for(ResourcePack resourcePack : ResourcePack.PACKS.values()) {
            ResourcePackManifest.Header header = resourcePack.getManifest().getHeader();
            String version = header.getVersion()[0] + "." + header.getVersion()[1] + "." + header.getVersion()[2];
            resourcePacksInfo.getResourcePackInfos().add(new ResourcePacksInfoPacket.Entry(header.getUuid().toString(), version, resourcePack.getFile().length(), "", "", "", false));
        }
        resourcePacksInfo.setForcedToAccept(true);
        session.getUpstream().sendPacket(resourcePacksInfo);
        return true;
    }

    @Override
    public boolean handle(ResourcePackClientResponsePacket packet) {
        switch (packet.getStatus()) {
            case COMPLETED:
                session.connect(connector.getRemoteServer());
                connector.getLogger().info("Player connected with username " + session.getAuthData().getName());
                break;

            case SEND_PACKS:
                for(String id : packet.getPackIds()) {
                    ResourcePackDataInfoPacket data = new ResourcePackDataInfoPacket();
                    ResourcePack pack = ResourcePack.PACKS.get(id.split("_")[0]);
                    ResourcePackManifest.Header header = pack.getManifest().getHeader();

                    data.setPackId(header.getUuid());
                    data.setChunkCount(pack.getFile().length()/ResourcePack.CHUNK_SIZE);
                    data.setCompressedPackSize(pack.getFile().length());
                    data.setMaxChunkSize(ResourcePack.CHUNK_SIZE);
                    data.setHash(pack.getSha256());

                    session.getUpstream().sendPacket(data);
                }
                break;

            case HAVE_ALL_PACKS:
                ResourcePackStackPacket stackPacket = new ResourcePackStackPacket();

                stackPacket.setExperimental(false);
                stackPacket.setForcedToAccept(true);
                stackPacket.setGameVersion(GeyserConnector.BEDROCK_PACKET_CODEC.getMinecraftVersion());
                for(ResourcePack pack : ResourcePack.PACKS.values()) {
                    ResourcePackManifest.Header header = pack.getManifest().getHeader();
                    String version = header.getVersion()[0] + "." + header.getVersion()[1] + "." + header.getVersion()[2];
                    stackPacket.getResourcePacks().add(new ResourcePackStackPacket.Entry(header.getUuid().toString(), version, ""));
                }
                session.getUpstream().sendPacket(stackPacket);
                break;

            default:
                session.disconnect("disconnectionScreen.resourcePack");
                break;
        }

        return true;
    }

    @Override
    public boolean handle(ModalFormResponsePacket packet) {
        return LoginEncryptionUtils.authenticateFromForm(session, connector, packet.getFormId(), packet.getFormData());
    }

    private boolean couldLoginUserByName(String bedrockUsername) {
        if (connector.getConfig().getUserAuths() != null) {
            IGeyserConfiguration.IUserAuthenticationInfo info = connector.getConfig().getUserAuths().get(bedrockUsername);

            if (info != null) {
                connector.getLogger().info("using stored credentials for bedrock user " + session.getAuthData().getName());
                session.authenticate(info.getEmail(), info.getPassword());

                // TODO send a message to bedrock user telling them they are connected (if nothing like a motd
                //      somes from the Java server w/in a few seconds)
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        if (!session.isLoggedIn() && !session.isLoggingIn() && session.getConnector().getAuthType() == AuthType.ONLINE) {
            // TODO it is safer to key authentication on something that won't change (UUID, not username)
            if (!couldLoginUserByName(session.getAuthData().getName())) {
                LoginEncryptionUtils.showLoginWindow(session);
            }
            // else we were able to log the user in
            return true;
        }
        if (session.isLoggingIn()) {
            session.sendMessage("Please wait until you are logged in...");
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
        ResourcePack pack = ResourcePack.PACKS.get(data.getPackId().toString());

        data.setChunkIndex(packet.getChunkIndex());
        data.setProgress(packet.getChunkIndex()*ResourcePack.CHUNK_SIZE);
        data.setPackVersion(packet.getPackVersion());
        data.setPackId(packet.getPackId());
        byte[] packData = new byte[(int) MathUtils.constrain(pack.getFile().length(), 0, ResourcePack.CHUNK_SIZE)];

        try (InputStream inputStream = new FileInputStream(pack.getFile())) {
            int offset = packet.getChunkIndex()*ResourcePack.CHUNK_SIZE;

            inputStream.read(packData, offset, packData.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.setData(packData);

        session.getUpstream().sendPacket(data);
        return true;
    }
}
