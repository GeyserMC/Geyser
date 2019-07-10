/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 3, 29 June 2007
 *
 * Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 *
 * You can view the LICENCE file for details.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network;

import com.nimbusds.jose.JWSObject;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.packet.PlayStatusPacket;
import com.nukkitx.protocol.bedrock.packet.ResourcePackClientResponsePacket;
import com.nukkitx.protocol.bedrock.packet.ResourcePackStackPacket;
import com.nukkitx.protocol.bedrock.packet.ResourcePacksInfoPacket;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.remote.RemoteJavaServer;
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
}