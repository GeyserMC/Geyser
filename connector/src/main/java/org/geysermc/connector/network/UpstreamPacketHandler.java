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

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.configuration.UserAuthenticationInfo;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.Registry;
import org.geysermc.connector.utils.LoginEncryptionUtils;

public class UpstreamPacketHandler extends LoggingPacketHandler {

    public UpstreamPacketHandler(GeyserConnector connector, GeyserSession session) {
        super(connector, session);
    }

    private boolean translateAndDefault(BedrockPacket packet) {
        Registry.BEDROCK.translate(packet.getClass(), packet, session);
        return defaultHandler(packet);
    }

    @Override
    public boolean handle(LoginPacket loginPacket) {
        // TODO: Implement support for multiple protocols
        if (loginPacket.getProtocolVersion() != GeyserConnector.BEDROCK_PACKET_CODEC.getProtocolVersion()) {
            connector.getLogger().debug("unsupported");
            session.getUpstream().disconnect("Unsupported Bedrock version. Are you running an outdated version?");
            return true;
        }

        LoginEncryptionUtils.encryptPlayerConnection(connector, session, loginPacket);

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
    public boolean handle(ModalFormResponsePacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        return LoginEncryptionUtils.authenticateFromForm(session, connector, packet.getFormData());
    }

    private boolean couldLoginUserByName(String bedrockUsername) {
        if (connector.getConfig().getUserAuths() != null) {
            UserAuthenticationInfo info = connector.getConfig().getUserAuths().get(bedrockUsername);

            if (info != null) {
                connector.getLogger().info("using stored credentials for bedrock user " + session.getAuthenticationData().getName());
                session.authenticate(info.email, info.password);

                // TODO send a message to bedrock user telling them they are connected (if nothing like a motd
                //      somes from the Java server w/in a few seconds)
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        connector.getLogger().debug("Handled packet: " + packet.getClass().getSimpleName());
        if (!session.isLoggedIn() && !session.isLoggingIn()) {
            // TODO it is safer to key authentication on something that won't change (UUID, not username)
            if (!couldLoginUserByName(session.getAuthenticationData().getName())) {
                LoginEncryptionUtils.showLoginWindow(session);
            }
            // else we were able to log the user in
            return true;
        } else if (session.isLoggingIn()) {
            session.sendMessage("Please wait until you are logged in...");
        }

        return translateAndDefault(packet);
    }

    @Override
    public boolean handle(AnimatePacket packet) {
        return translateAndDefault(packet);
    }

    @Override
    public boolean handle(CommandRequestPacket packet) {
        return translateAndDefault(packet);
    }

    @Override
    public boolean handle(TextPacket packet) {
        return translateAndDefault(packet);
    }

    @Override
    public boolean handle(MobEquipmentPacket packet) {
        return translateAndDefault(packet);
    }

    @Override
    public boolean handle(PlayerActionPacket packet) {
        return translateAndDefault(packet);
    }

    @Override
    public boolean handle(InventoryTransactionPacket packet) {
        return translateAndDefault(packet);
    }
}