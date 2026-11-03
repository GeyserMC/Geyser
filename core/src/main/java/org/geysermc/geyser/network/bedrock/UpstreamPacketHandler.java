/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

import io.netty.buffer.Unpooled;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.protocol.bedrock.data.ResourcePackType;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.ModalFormResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackChunkDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackChunkRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackClientResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackDataInfoPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.ResourcePackManifest;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;
import org.geysermc.geyser.event.type.SessionLoadResourcePacksEventImpl;
import org.geysermc.geyser.pack.GeyserResourcePack;
import org.geysermc.geyser.pack.ResourcePackHolder;
import org.geysermc.geyser.pack.url.GeyserUrlPackCodec;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.loader.ResourcePackLoader;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.PendingMicrosoftAuthentication;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.MathUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UpstreamPacketHandler extends LoggingPacketHandler {

    private boolean finishedResourcePackSending = false;
    private boolean sentResourcePackStack = false;
    private final Deque<String> packsToSend = new ArrayDeque<>();
    private final Map<UUID, BitSet> sentChunks = new HashMap<>();
    private final Set<String> requestedPacks = new HashSet<>();
    private SessionLoadResourcePacksEventImpl resourcePackLoadEvent;

    UpstreamPacketHandler(GeyserImpl geyser, GeyserSession session) {
        super(geyser, session);
    }

    private PacketSignal translateAndDefault(BedrockPacket packet) {
        Registries.BEDROCK_PACKET_TRANSLATORS.translate(packet.getClass(), packet, session, false);
        return PacketSignal.HANDLED; // PacketSignal.UNHANDLED will log a WARN publicly
    }

    @Override
    PacketSignal defaultHandler(BedrockPacket packet) {
        return translateAndDefault(packet);
    }

    /**
     * Starts the resource pack phase, once {@link LoginPacketHandler} has finished the login.
     */
    void sendResourcePacksInfo() {
        this.resourcePackLoadEvent = new SessionLoadResourcePacksEventImpl(session);
        this.geyser.eventBus().fireEventElseKick(this.resourcePackLoadEvent, session);
        if (session.isClosed()) {
            // Can happen if an error occurs in the resource pack event; that'll disconnect the player
            return;
        }
        session.integratedPackActive(resourcePackLoadEvent.isIntegratedPackActive());

        ResourcePacksInfoPacket resourcePacksInfo = new ResourcePacksInfoPacket();
        resourcePacksInfo.getResourcePackInfos().addAll(this.resourcePackLoadEvent.infoPacketEntries());
        resourcePacksInfo.setVibrantVisualsForceDisabled(!session.isAllowVibrantVisuals());

        resourcePacksInfo.setForcedToAccept(GeyserImpl.getInstance().config().gameplay().forceResourcePacks() ||
            resourcePackLoadEvent.isIntegratedPackActive());
        resourcePacksInfo.setWorldTemplateId(new UUID(0, 0));
        resourcePacksInfo.setWorldTemplateVersion("");

        session.sendUpstreamPacket(resourcePacksInfo);

        GeyserLocale.loadGeyserLocale(session.locale());
    }

    @Override
    public PacketSignal handle(ResourcePackClientResponsePacket packet) {
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
                    // We must spawn the white world
                    session.connect();
                }
                geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.connect", session.getAuthData().name() +
                    " (" + session.protocolVersion() + ")"));
            }
            case SEND_PACKS -> {
                // Bedrock clients can send empty "send_packs" responses, in which case we shouldn't send anything back
                if (!packet.getPackIds().isEmpty()) {
                    // Each pack is sent once, and no more can be asked for than were offered
                    boolean added = false;
                    for (String id : packet.getPackIds()) {
                        if (requestedPacks.size() >= resourcePackLoadEvent.getPacks().size()) {
                            break;
                        }
                        if (requestedPacks.add(id)) {
                            packsToSend.add(id);
                            added = true;
                        }
                    }
                    if (added) {
                        sendPackDataInfo(packsToSend.pop());
                    }
                    return PacketSignal.HANDLED;
                }
            }
            case HAVE_ALL_PACKS -> {
                // Only needed once
                if (!sentResourcePackStack) {
                    sentResourcePackStack = true;

                    ResourcePackStackPacket stackPacket = new ResourcePackStackPacket();
                    stackPacket.setExperimentsPreviouslyToggled(false);
                    stackPacket.setForcedToAccept(false); // Leaving this as false allows the player to choose to download or not
                    stackPacket.setGameVersion("*");
                    stackPacket.getResourcePacks().addAll(this.resourcePackLoadEvent.orderedPacks());

                    session.sendUpstreamPacket(stackPacket);
                }
            }
            case REFUSED -> session.disconnect("disconnectionScreen.resourcePack");
            default -> {
                GeyserImpl.getInstance().getLogger().debug("received unknown status packet: " + packet);
                session.disconnect("disconnectionScreen.resourcePack");
            }
        }

        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ModalFormResponsePacket packet) {
        if (session.getUpstream().isClosed() || session.isClosed()) {
            return PacketSignal.HANDLED;
        }
        session.executeInEventLoop(() -> session.getFormCache().handleResponse(packet));
        return PacketSignal.HANDLED;
    }

    private boolean couldLoginUserByName(String bedrockUsername) {
        if (geyser.config().savedUserLogins().contains(bedrockUsername)) {
            String authChain = geyser.authChainFor(bedrockUsername);
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

    @Override
    public PacketSignal handle(PlayerAuthInputPacket packet) {
        // This doesn't catch rotation, but for a niche case I don't exactly want to cache rotation...
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

    @Override
    public PacketSignal handle(ResourcePackChunkRequestPacket packet) {
        if (session.getUpstream().isClosed() || session.isClosed()) {
            return PacketSignal.HANDLED;
        }

        sendChunk(packet);
        return PacketSignal.HANDLED;
    }

    private void sendChunk(ResourcePackChunkRequestPacket packet) {
        ResourcePackHolder holder = this.resourcePackLoadEvent.getPacks().get(packet.getPackId());
        if (holder == null) {
            GeyserImpl.getInstance().getLogger().debug("Client %s tried to request pack id %s not sent to it!",
                session.bedrockUsername(), packet.getPackId());
            session.disconnect("disconnectionScreen.resourcePack");
            return;
        }

        PackCodec codec = holder.codec();
        // If a remote pack ends up here, that usually implies that a client was not able to download the pack
        if (codec instanceof GeyserUrlPackCodec urlPackCodec) {
            ResourcePackLoader.testRemotePack(session, urlPackCodec, holder);
            if (!resourcePackLoadEvent.value(holder.uuid(), ResourcePackOption.Type.FALLBACK, true)) {
                session.disconnect("Unable to provide downloaded resource pack. Contact an administrator!");
                return;
            }
        } else if (finishedResourcePackSending) {
            GeyserImpl.getInstance().getLogger().warning("Received resource pack chunk packet after stage completed! " + packet);
            session.disconnect("Duplicate resource pack packet received!");
            return;
        }

        int index = packet.getChunkIndex();
        long offset = (long) index * GeyserResourcePack.CHUNK_SIZE;
        BitSet sent = sentChunks.computeIfAbsent(packet.getPackId(), id -> new BitSet());
        if (index < 0 || offset >= codec.size() || sent.get(index)) {
            return;
        }
        sent.set(index);

        ResourcePackChunkDataPacket data = new ResourcePackChunkDataPacket();
        data.setChunkIndex(index);
        data.setProgress(offset);
        data.setPackVersion(packet.getPackVersion());
        data.setPackId(packet.getPackId());

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

        // Also flushes packets
        session.sendUpstreamPacketImmediately(data);

        // Check if it is the last chunk and send next pack in queue when available.
        if (remainingSize <= GeyserResourcePack.CHUNK_SIZE && !packsToSend.isEmpty()) {
            sendPackDataInfo(packsToSend.pop());
        }
    }

    private void sendPackDataInfo(String id) {
        ResourcePackDataInfoPacket data = new ResourcePackDataInfoPacket();
        String[] packID = id.split("_");

        if (packID.length < 2) {
            GeyserImpl.getInstance().getLogger().debug("Client %s tried to request invalid pack id %s!",
                session.bedrockUsername(), Arrays.toString(packID));
            session.disconnect("disconnectionScreen.resourcePack");
            return;
        }

        UUID packId;
        try {
            packId = UUID.fromString(packID[0]);
        } catch (IllegalArgumentException e) {
            GeyserImpl.getInstance().getLogger().debug("Client %s tried to request pack with an invalid id %s)",
                session.bedrockUsername(), id);
            session.disconnect("disconnectionScreen.resourcePack");
            return;
        }

        ResourcePackHolder holder = this.resourcePackLoadEvent.getPacks().get(packId);
        if (holder == null) {
            GeyserImpl.getInstance().getLogger().debug("Client %s tried to request pack id %s not sent to it!",
                session.bedrockUsername(), id);
            session.disconnect("disconnectionScreen.resourcePack");
            return;
        }

        ResourcePack pack = holder.pack();
        PackCodec codec = pack.codec();
        ResourcePackManifest.Header header = pack.manifest().header();

        data.setPackId(header.uuid());
        data.setChunkCount(chunkCount(codec));
        data.setCompressedPackSize(codec.size());
        data.setMaxChunkSize(GeyserResourcePack.CHUNK_SIZE);
        data.setHash(codec.sha256());
        data.setPackVersion(packID[1]);
        data.setPremium(false);
        data.setType(ResourcePackType.RESOURCES);

        session.sendUpstreamPacket(data);
    }

    private static int chunkCount(PackCodec codec) {
        return (int) Math.ceil(codec.size() / (double) GeyserResourcePack.CHUNK_SIZE);
    }
}
