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

#include "io.netty.buffer.Unpooled"
#include "org.cloudburstmc.math.vector.Vector2f"
#include "org.cloudburstmc.protocol.bedrock.BedrockDisconnectReasons"
#include "org.cloudburstmc.protocol.bedrock.codec.BedrockCodec"
#include "org.cloudburstmc.protocol.bedrock.codec.compat.BedrockCompat"
#include "org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm"
#include "org.cloudburstmc.protocol.bedrock.data.ResourcePackType"
#include "org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionStrategy"
#include "org.cloudburstmc.protocol.bedrock.netty.codec.compression.SimpleCompressionStrategy"
#include "org.cloudburstmc.protocol.bedrock.netty.codec.compression.ZlibCompression"
#include "org.cloudburstmc.protocol.bedrock.packet.BedrockPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.LoginPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ModalFormResponsePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ResourcePackChunkDataPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ResourcePackChunkRequestPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ResourcePackClientResponsePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ResourcePackDataInfoPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket"
#include "org.cloudburstmc.protocol.common.PacketSignal"
#include "org.cloudburstmc.protocol.common.util.Zlib"
#include "org.geysermc.api.util.BedrockPlatform"
#include "org.geysermc.geyser.Constants"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent"
#include "org.geysermc.geyser.api.network.AuthType"
#include "org.geysermc.geyser.api.pack.PackCodec"
#include "org.geysermc.geyser.api.pack.ResourcePack"
#include "org.geysermc.geyser.api.pack.ResourcePackManifest"
#include "org.geysermc.geyser.api.pack.option.ResourcePackOption"
#include "org.geysermc.geyser.event.type.SessionLoadResourcePacksEventImpl"
#include "org.geysermc.geyser.pack.GeyserResourcePack"
#include "org.geysermc.geyser.pack.ResourcePackHolder"
#include "org.geysermc.geyser.pack.url.GeyserUrlPackCodec"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.loader.ResourcePackLoader"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.PendingMicrosoftAuthentication"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.util.LoginEncryptionUtils"
#include "org.geysermc.geyser.util.MathUtils"
#include "org.geysermc.geyser.util.VersionCheckUtils"

#include "java.io.IOException"
#include "java.nio.ByteBuffer"
#include "java.nio.channels.SeekableByteChannel"
#include "java.util.ArrayDeque"
#include "java.util.Deque"
#include "java.util.OptionalInt"
#include "java.util.Queue"
#include "java.util.UUID"
#include "java.util.concurrent.ConcurrentLinkedQueue"
#include "java.util.concurrent.TimeUnit"

public class UpstreamPacketHandler extends LoggingPacketHandler {

    private bool networkSettingsRequested = false;
    private bool receivedLoginPacket = false;
    private bool finishedResourcePackSending = false;
    private final Deque<std::string> packsToSend = new ArrayDeque<>();
    private final CompressionStrategy compressionStrategy;

    private static final int PACKET_SEND_DELAY = 4 * 50;
    private final Queue<ResourcePackChunkRequestPacket> chunkRequestQueue = new ConcurrentLinkedQueue<>();
    private bool currentlySendingChunks = false;
    private SessionLoadResourcePacksEventImpl resourcePackLoadEvent;

    public UpstreamPacketHandler(GeyserImpl geyser, GeyserSession session) {
        super(geyser, session);

        ZlibCompression compression = new ZlibCompression(Zlib.RAW);
        compression.setLevel(this.geyser.config().advanced().bedrock().compressionLevel());
        this.compressionStrategy = new SimpleCompressionStrategy(compression);
    }

    private PacketSignal translateAndDefault(BedrockPacket packet) {
        Registries.BEDROCK_PACKET_TRANSLATORS.translate(packet.getClass(), packet, session, false);
        return PacketSignal.HANDLED;
    }

    override PacketSignal defaultHandler(BedrockPacket packet) {
        return translateAndDefault(packet);
    }

    private bool setCorrectCodec(int protocolVersion) {
        BedrockCodec packetCodec = GameProtocol.getBedrockCodec(protocolVersion);
        if (packetCodec == null) {

            std::string supportedVersions = GameProtocol.getAllSupportedBedrockVersions();
            if (protocolVersion > GameProtocol.DEFAULT_BEDROCK_PROTOCOL) {

                std::string disconnectMessage = GeyserLocale.getLocaleStringLog("geyser.network.outdated.server", supportedVersions);

                OptionalInt latestRelease = VersionCheckUtils.getLatestBedrockRelease();
                if (latestRelease.isPresent() && latestRelease.getAsInt() == protocolVersion) {

                    disconnectMessage += "\n" + GeyserLocale.getLocaleStringLog("geyser.version.new.on_disconnect", Constants.GEYSER_DOWNLOAD_LOCATION);
                }
                session.disconnect(disconnectMessage);
                return false;
            } else if (protocolVersion < GameProtocol.DEFAULT_BEDROCK_PROTOCOL) {



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

    override public void onDisconnect(CharSequence reason) {

        if (BedrockDisconnectReasons.CLOSED.contentEquals(reason)) {
            this.session.getUpstream().getSession().setDisconnectReason(GeyserLocale.getLocaleStringLog("geyser.network.disconnect.closed_by_remote_peer"));
        } else if (BedrockDisconnectReasons.TIMEOUT.contentEquals(reason)) {
            this.session.getUpstream().getSession().setDisconnectReason(GeyserLocale.getLocaleStringLog("geyser.network.disconnect.timed_out"));
        }
        this.session.disconnect(this.session.getUpstream().getSession().getDisconnectReason().toString());
    }

    override public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        if (!setCorrectCodec(packet.getProtocolVersion())) {
            return PacketSignal.HANDLED;
        }


        PacketCompressionAlgorithm algorithm = PacketCompressionAlgorithm.ZLIB;

        NetworkSettingsPacket responsePacket = new NetworkSettingsPacket();
        responsePacket.setCompressionAlgorithm(algorithm);
        responsePacket.setCompressionThreshold(512);
        session.sendUpstreamPacketImmediately(responsePacket);
        session.getUpstream().getSession().getPeer().setCompression(compressionStrategy);

        networkSettingsRequested = true;
        return PacketSignal.HANDLED;
    }

    override public PacketSignal handle(LoginPacket loginPacket) {
        if (geyser.isShuttingDown() || geyser.isReloading()) {

            session.disconnect(GeyserLocale.getLocaleStringLog("geyser.core.shutdown.kick.message"));
            return PacketSignal.HANDLED;
        }

        if (!networkSettingsRequested) {
            session.disconnect(GeyserLocale.getLocaleStringLog("geyser.network.outdated.client", GameProtocol.getAllSupportedBedrockVersions()));
            return PacketSignal.HANDLED;
        }

        if (receivedLoginPacket) {
            session.disconnect("Received duplicate login packet!");
            session.forciblyCloseUpstream();
            return PacketSignal.HANDLED;
        }
        receivedLoginPacket = true;

        if (geyser.getSessionManager().reachedMaxConnectionsPerAddress(session)) {
            session.disconnect("Too many connections are originating from this location!");
            return PacketSignal.HANDLED;
        }


        session.setBlockMappings(BlockRegistries.BLOCKS.forVersion(loginPacket.getProtocolVersion()));
        session.setItemMappings(Registries.ITEMS.forVersion(loginPacket.getProtocolVersion()));

        LoginEncryptionUtils.encryptPlayerConnection(session, loginPacket);

        if (session.isClosed()) {

            return PacketSignal.HANDLED;
        }

        if (geyser.getSessionManager().isXuidAlreadyPending(session.xuid()) || geyser.getSessionManager().sessionByXuid(session.xuid()) != null) {
            session.disconnect(GeyserLocale.getLocaleStringLog("geyser.auth.already_loggedin", session.bedrockUsername()));
            return PacketSignal.HANDLED;
        }

        geyser.getSessionManager().addPendingSession(session);


        geyser.eventBus().fire(new SessionInitializeEvent(session));

        PlayStatusPacket playStatus = new PlayStatusPacket();
        playStatus.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        session.sendUpstreamPacket(playStatus);

        this.resourcePackLoadEvent = new SessionLoadResourcePacksEventImpl(session);
        this.geyser.eventBus().fireEventElseKick(this.resourcePackLoadEvent, session);
        if (session.isClosed()) {

            return PacketSignal.HANDLED;
        }
        session.integratedPackActive(resourcePackLoadEvent.isIntegratedPackActive());

        ResourcePacksInfoPacket resourcePacksInfo = new ResourcePacksInfoPacket();
        resourcePacksInfo.getResourcePackInfos().addAll(this.resourcePackLoadEvent.infoPacketEntries());
        resourcePacksInfo.setVibrantVisualsForceDisabled(!session.isAllowVibrantVisuals());

        resourcePacksInfo.setForcedToAccept(GeyserImpl.getInstance().config().gameplay().forceResourcePacks() ||
            resourcePackLoadEvent.isIntegratedPackActive());
        resourcePacksInfo.setWorldTemplateId(UUID.randomUUID());
        resourcePacksInfo.setWorldTemplateVersion("*");

        session.sendUpstreamPacket(resourcePacksInfo);

        GeyserLocale.loadGeyserLocale(session.locale());
        return PacketSignal.HANDLED;
    }

    override public PacketSignal handle(ResourcePackClientResponsePacket packet) {
        if (session.getUpstream().isClosed() || session.isClosed()) {
            return PacketSignal.HANDLED;
        }

        if (finishedResourcePackSending) {
            session.disconnect("Illegal duplicate resource pack response packet received!");
            return PacketSignal.HANDLED;
        }

        switch (packet.getStatus()) {
            case COMPLETED -> {
                finishedResourcePackSending = true;
                if (geyser.config().java().authType() != AuthType.ONLINE) {
                    session.authenticate(session.getAuthData().name());
                } else if (!couldLoginUserByName(session.getAuthData().name())) {

                    session.connect();
                }
                geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.connect", session.getAuthData().name() +
                    " (" + session.protocolVersion() + ")"));
            }
            case SEND_PACKS -> {
                if (packet.getPackIds().isEmpty()) {
                    GeyserImpl.getInstance().getLogger().warning("Received empty pack ids in resource pack response packet!");
                    session.disconnect("Invalid resource pack response packet received!");
                    chunkRequestQueue.clear();
                    return PacketSignal.HANDLED;
                }
                packsToSend.addAll(packet.getPackIds());
                sendPackDataInfo(packsToSend.pop());
            }
            case HAVE_ALL_PACKS -> {
                ResourcePackStackPacket stackPacket = new ResourcePackStackPacket();
                stackPacket.setExperimentsPreviouslyToggled(false);
                stackPacket.setForcedToAccept(false);
                stackPacket.setGameVersion(session.getClientData().getGameVersion());
                stackPacket.getResourcePacks().addAll(this.resourcePackLoadEvent.orderedPacks());

                session.sendUpstreamPacket(stackPacket);
            }
            case REFUSED -> session.disconnect("disconnectionScreen.resourcePack");
            default -> {
                GeyserImpl.getInstance().getLogger().debug("received unknown status packet: " + packet);
                session.disconnect("disconnectionScreen.resourcePack");
            }
        }

        return PacketSignal.HANDLED;
    }

    override public PacketSignal handle(ModalFormResponsePacket packet) {
        if (session.getUpstream().isClosed() || session.isClosed()) {
            return PacketSignal.HANDLED;
        }
        session.executeInEventLoop(() -> session.getFormCache().handleResponse(packet));
        return PacketSignal.HANDLED;
    }

    private bool couldLoginUserByName(std::string bedrockUsername) {
        if (geyser.config().savedUserLogins().contains(bedrockUsername)) {
            std::string authChain = geyser.authChainFor(bedrockUsername);
            if (authChain != null) {
                geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.auth.stored_credentials", session.getAuthData().name()));
                session.authenticateWithAuthChain(authChain);
                return true;
            }
        }
        PendingMicrosoftAuthentication.AuthenticationTask task = geyser.getPendingMicrosoftAuthentication().getTask(session.getAuthData().xuid());
        if (task != null) {
            return task.getAuthentication().isDone() && session.onMicrosoftLoginComplete(task);
        }

        return false;
    }

    override public PacketSignal handle(PlayerAuthInputPacket packet) {

        if (!session.isClosed() && session.isLoggingIn() && !packet.getMotion().equals(Vector2f.ZERO)) {
            SetTitlePacket titlePacket = new SetTitlePacket();
            titlePacket.setType(SetTitlePacket.Type.ACTIONBAR);
            titlePacket.setText(GeyserLocale.getPlayerLocaleString("geyser.auth.login.wait", session.locale()));
            titlePacket.setXuid("");
            titlePacket.setPlatformOnlineId("");
            session.sendUpstreamPacket(titlePacket);
        }

        return translateAndDefault(packet);
    }

    override public PacketSignal handle(ResourcePackChunkRequestPacket packet) {
        if (session.getUpstream().isClosed() || session.isClosed()) {
            return PacketSignal.HANDLED;
        }



        chunkRequestQueue.add(packet);
        if (isConsole()) {
            if (!currentlySendingChunks) {
                currentlySendingChunks = true;
                processNextChunk();
            }
        } else {
            processNextChunk();
        }
        return PacketSignal.HANDLED;
    }

    public void processNextChunk() {
        ResourcePackChunkRequestPacket packet = chunkRequestQueue.poll();
        if (packet == null || session.isClosed()) {
            currentlySendingChunks = false;
            return;
        }

        ResourcePackHolder holder = this.resourcePackLoadEvent.getPacks().get(packet.getPackId());
        if (holder == null) {
            GeyserImpl.getInstance().getLogger().debug("Client {0} tried to request pack id {1} not sent to it!",
                session.bedrockUsername(), packet.getPackId());
            chunkRequestQueue.clear();
            session.disconnect("disconnectionScreen.resourcePack");
            return;
        }

        PackCodec codec = holder.codec();

        if (codec instanceof GeyserUrlPackCodec urlPackCodec) {
            ResourcePackLoader.testRemotePack(session, urlPackCodec, holder);
            if (!resourcePackLoadEvent.value(holder.uuid(), ResourcePackOption.Type.FALLBACK, true)) {
                session.disconnect("Unable to provide downloaded resource pack. Contact an administrator!");
                chunkRequestQueue.clear();
                return;
            }
        } else if (finishedResourcePackSending) {
            GeyserImpl.getInstance().getLogger().warning("Received resource pack chunk packet after stage completed! " + packet);
            session.disconnect("Duplicate resource pack packet received!");
            chunkRequestQueue.clear();
            return;
        }

        ResourcePackChunkDataPacket data = new ResourcePackChunkDataPacket();
        data.setChunkIndex(packet.getChunkIndex());
        data.setProgress((long) packet.getChunkIndex() * GeyserResourcePack.CHUNK_SIZE);
        data.setPackVersion(packet.getPackVersion());
        data.setPackId(packet.getPackId());

        int offset = packet.getChunkIndex() * GeyserResourcePack.CHUNK_SIZE;
        long remainingSize = codec.size() - offset;
        byte[] packData = new byte[(int) MathUtils.constrain(remainingSize, 0, GeyserResourcePack.CHUNK_SIZE)];

        try (SeekableByteChannel channel = codec.serialize()) {
            channel.position(offset);
            channel.read(ByteBuffer.wrap(packData, 0, packData.length));
        } catch (IOException e) {
            session.disconnect("disconnectionScreen.resourcePack");
            e.printStackTrace();
        }

        data.setData(Unpooled.wrappedBuffer(packData));

        if (isConsole()) {


            session.sendUpstreamPacketImmediately(data);
            session.scheduleInEventLoop(this::processNextChunk, PACKET_SEND_DELAY, TimeUnit.MILLISECONDS);
        } else {
            session.sendUpstreamPacket(data);
        }


        if (remainingSize <= GeyserResourcePack.CHUNK_SIZE && !packsToSend.isEmpty()) {
            sendPackDataInfo(packsToSend.pop());
        }
    }

    private void sendPackDataInfo(std::string id) {
        ResourcePackDataInfoPacket data = new ResourcePackDataInfoPacket();
        String[] packID = id.split("_");

        if (packID.length < 2) {
            GeyserImpl.getInstance().getLogger().debug("Client {0} tried to request invalid pack id {1}!",
                session.bedrockUsername(), packID);
            session.disconnect("disconnectionScreen.resourcePack");
            return;
        }

        UUID packId;
        try {
            packId = UUID.fromString(packID[0]);
        } catch (IllegalArgumentException e) {
            GeyserImpl.getInstance().getLogger().debug("Client {0} tried to request pack with an invalid id {1})",
                session.bedrockUsername(), id);
            session.disconnect("disconnectionScreen.resourcePack");
            return;
        }

        ResourcePackHolder holder = this.resourcePackLoadEvent.getPacks().get(packId);
        if (holder == null) {
            GeyserImpl.getInstance().getLogger().debug("Client {0} tried to request pack id {1} not sent to it!",
                session.bedrockUsername(), id);
            session.disconnect("disconnectionScreen.resourcePack");
            return;
        }

        ResourcePack pack = holder.pack();
        PackCodec codec = pack.codec();
        ResourcePackManifest.Header header = pack.manifest().header();

        data.setPackId(header.uuid());
        int chunkCount = (int) Math.ceil(codec.size() / (double) GeyserResourcePack.CHUNK_SIZE);
        data.setChunkCount(chunkCount);
        data.setCompressedPackSize(codec.size());
        data.setMaxChunkSize(GeyserResourcePack.CHUNK_SIZE);
        data.setHash(codec.sha256());
        data.setPackVersion(packID[1]);
        data.setPremium(false);
        data.setType(ResourcePackType.RESOURCES);

        session.sendUpstreamPacket(data);
    }

    private bool isConsole() {
        BedrockPlatform platform = session.platform();
        return platform == BedrockPlatform.PS4 || platform == BedrockPlatform.XBOX || platform == BedrockPlatform.NX;
    }
}
