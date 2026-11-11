/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.bedrock;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.compat.BedrockCompat;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.SimpleCompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.ZlibCompression;
import org.cloudburstmc.protocol.bedrock.netty.codec.encryption.BedrockEncryptionDecoder;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientToServerHandshakePacket;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.util.Zlib;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.LoginEncryptionUtils;
import org.geysermc.geyser.util.VersionCheckUtils;

import java.util.OptionalInt;

/**
 * Handles the login sequence, then hands the connection to {@link UpstreamPacketHandler}. Any packet but the one
 * the sequence expects next ends the connection.
 */
public class LoginPacketHandler extends LoggingPacketHandler {
    private static final String ENCRYPTED_INPUT_MARKER = "geyser-encrypted-input-marker";

    private final CompressionStrategy compressionStrategy;
    private Stage stage = Stage.NETWORK_SETTINGS;
    /**
     * Whether a batch has been read through the decryptor.
     */
    private boolean encryptedInputSeen = false;
    private int loginProtocolVersion;

    public LoginPacketHandler(GeyserImpl geyser, GeyserSession session) {
        super(geyser, session);

        ZlibCompression compression = new ZlibCompression(Zlib.RAW);
        compression.setLevel(this.geyser.config().advanced().bedrock().compressionLevel());
        this.compressionStrategy = new SimpleCompressionStrategy(compression);
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        if (session.isClosed()) {
            // Disconnecting, including the rest of the batch that caused it
            return PacketSignal.HANDLED;
        }

        if (!stage.expects(packet) || (stage == Stage.HANDSHAKE && !encryptedInputSeen)) {
            geyser.getLogger().debug("Disconnecting on " + packet.getClass().getSimpleName() + " in login stage " + stage);
            // Not forcibly closed, so the client gets the reason. The session drops further input and closes
            // once the client leaves, or after a timeout.
            session.disconnect("Invalid packet received!");
            return PacketSignal.HANDLED;
        }
        return super.handlePacket(packet);
    }

    private boolean setCorrectCodec(int protocolVersion) {
        BedrockCodec packetCodec = GameProtocol.getBedrockCodec(protocolVersion);
        if (packetCodec == null) {
            // None of our Bedrock codecs support this client version, so we can simply compare it to our default protocol.
            String supportedVersions = GameProtocol.getAllSupportedBedrockVersions();
            if (protocolVersion > GameProtocol.DEFAULT_BEDROCK_PROTOCOL) {
                // Too early to determine session locale
                String disconnectMessage = GeyserLocale.getLocaleStringLog("geyser.network.outdated.server", supportedVersions);
                // If the latest release matches this version, then let the user know.
                OptionalInt latestRelease = VersionCheckUtils.getLatestBedrockRelease();
                if (latestRelease.isPresent() && latestRelease.getAsInt() == protocolVersion) {
                    // Random note: don't make the disconnect message too long or Bedrock will cut it off on smaller screens
                    disconnectMessage += "\n" + GeyserLocale.getLocaleStringLog("geyser.version.new.on_disconnect", Constants.GEYSER_DOWNLOAD_LOCATION);
                }
                session.disconnect(disconnectMessage);
                return false;
            } else if (protocolVersion < GameProtocol.DEFAULT_BEDROCK_PROTOCOL) {
                // A note on the following line: various older client versions have different forms of DisconnectPacket.
                // Using only the latest BedrockCompat for such clients leads to inaccurate disconnect messages: https://github.com/GeyserMC/Geyser/issues/4378
                // This updates the BedrockCompat protocol if necessary:
                session.getUpstream().getSession().setCodec(BedrockCompat.disconnectCompat(protocolVersion));

                session.disconnect(GeyserLocale.getLocaleStringLog("geyser.network.outdated.client", supportedVersions));
                return false;
            } else {
                throw new IllegalStateException("Default codec of protocol version " + protocolVersion + " should have been found");
            }
        }

        session.getUpstream().getSession().setCodec(packetCodec);
        return true;
    }

    @Override
    public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        if (!setCorrectCodec(packet.getProtocolVersion())) {
            return PacketSignal.HANDLED;
        }

        // New since 1.19.30 - sent before login packet
        NetworkSettingsPacket responsePacket = new NetworkSettingsPacket();
        responsePacket.setCompressionAlgorithm(PacketCompressionAlgorithm.ZLIB);
        responsePacket.setCompressionThreshold(512);
        session.sendUpstreamPacketImmediately(responsePacket);
        session.getUpstream().getSession().getPeer().setCompression(compressionStrategy);

        stage = Stage.LOGIN;
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LoginPacket loginPacket) {
        if (geyser.isShuttingDown() || geyser.isReloading()) {
            // Don't allow new players in if we're no longer operating
            session.disconnect(GeyserLocale.getLocaleStringLog("geyser.core.shutdown.kick.message"));
            return PacketSignal.HANDLED;
        }

        stage = Stage.DONE;
        loginProtocolVersion = loginPacket.getProtocolVersion();

        boolean awaitingHandshake = LoginEncryptionUtils.encryptPlayerConnection(session, loginPacket);

        if (session.isClosed()) {
            // Can happen if Xbox validation fails
            session.forciblyCloseUpstream();
            return PacketSignal.HANDLED;
        }

        if (awaitingHandshake) {
            stage = Stage.HANDSHAKE;
            markEncryptedInput();
            return PacketSignal.HANDLED;
        }

        // The transport already bound the login to the client's key
        finishLogin();
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ClientToServerHandshakePacket packet) {
        stage = Stage.DONE;
        finishLogin();
        return PacketSignal.HANDLED;
    }

    /**
     * Notes the first batch read through the decryptor.
     */
    private void markEncryptedInput() {
        session.getUpstream().getSession().getPeer().getChannel().pipeline()
            .addAfter(BedrockEncryptionDecoder.NAME, ENCRYPTED_INPUT_MARKER, new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    encryptedInputSeen = true;
                    ctx.pipeline().remove(this);
                    ctx.fireChannelRead(msg);
                }
            });
    }

    private void finishLogin() {
        if (geyser.getSessionManager().isXuidAlreadyPending(session.xuid()) || geyser.getSessionManager().sessionByXuid(session.xuid()) != null) {
            session.disconnect(GeyserLocale.getLocaleStringLog("geyser.auth.already_loggedin", session.bedrockUsername()));
            return;
        }

        // Set the block translation based off of version
        session.setBlockMappings(BlockRegistries.BLOCKS.forVersion(loginProtocolVersion));
        session.setItemMappings(Registries.ITEMS.forVersion(loginProtocolVersion));

        geyser.getSessionManager().addPendingSession(session);

        // Fire SessionInitializeEvent here as we now know the client data
        geyser.eventBus().fire(new SessionInitializeEvent(session));

        PlayStatusPacket playStatus = new PlayStatusPacket();
        playStatus.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        session.sendUpstreamPacket(playStatus);

        UpstreamPacketHandler handler = new UpstreamPacketHandler(geyser, session);
        session.getUpstream().getSession().setPacketHandler(handler);
        handler.sendResourcePacksInfo();
    }

    /**
     * The login sequence. Each stage accepts only the packet that moves it on.
     */
    private enum Stage {
        NETWORK_SETTINGS(RequestNetworkSettingsPacket.class),
        LOGIN(LoginPacket.class),
        /**
         * Accepts the handshake only once it is read through the decryptor.
         */
        HANDSHAKE(ClientToServerHandshakePacket.class),
        /**
         * Handed to {@link UpstreamPacketHandler}, or disconnected.
         */
        DONE(null);

        private final @Nullable Class<? extends BedrockPacket> expected;

        Stage(@Nullable Class<? extends BedrockPacket> expected) {
            this.expected = expected;
        }

        boolean expects(BedrockPacket packet) {
            return expected != null && expected.isInstance(packet);
        }
    }
}
