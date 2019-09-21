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

package org.geysermc.connector.network.session;

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.data.GamePublishSetting;
import com.nukkitx.protocol.bedrock.data.GameRule;
import com.nukkitx.protocol.bedrock.packet.PlayStatusPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.packet.TextPacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.api.Player;
import org.geysermc.api.RemoteServer;
import org.geysermc.api.session.AuthData;
import org.geysermc.api.window.FormWindow;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.cache.*;
import org.geysermc.connector.network.translators.Registry;
import org.geysermc.connector.utils.Toolbox;

import java.net.InetSocketAddress;
import java.util.UUID;

@Getter
public class GeyserSession implements Player {
    private final GeyserConnector connector;
    private final BedrockServerSession upstream;
    private RemoteServer remoteServer;
    private Client downstream;
    private AuthData authenticationData;

    private PlayerEntity playerEntity;
    private PlayerInventory inventory;

    private ChunkCache chunkCache;
    private EntityCache entityCache;
    private InventoryCache inventoryCache;
    private ScoreboardCache scoreboardCache;
    private WindowCache windowCache;

    private DataCache<Packet> javaPacketCache;

    @Setter
    private Vector2i lastChunkPosition = null;
    @Setter
    private int renderDistance;

    private boolean loggedIn;
    private boolean loggingIn;

    @Setter
    private boolean spawned;
    private boolean closed;

    public GeyserSession(GeyserConnector connector, BedrockServerSession bedrockServerSession) {
        this.connector = connector;
        this.upstream = bedrockServerSession;

        this.chunkCache = new ChunkCache(this);
        this.entityCache = new EntityCache(this);
        this.inventoryCache = new InventoryCache(this);
        this.scoreboardCache = new ScoreboardCache(this);
        this.windowCache = new WindowCache(this);

        this.playerEntity = new PlayerEntity(new GameProfile(UUID.randomUUID(), "unknown"), -1, 1, new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0, 0, 0));
        this.inventory = new PlayerInventory();

        this.javaPacketCache = new DataCache<>();

        this.spawned = false;
        this.loggedIn = false;

        this.inventoryCache.getInventories().put(0, inventory);
    }

    public void connect(RemoteServer remoteServer) {
        // This has to be sent first so the player actually joins
        startGame();

        this.remoteServer = remoteServer;
        if (!(connector.getConfig().getRemote().getAuthType().hashCode() == "online".hashCode())) {
            connector.getLogger().info("Attempting to login using offline mode... authentication is disabled.");
            authenticate(authenticationData.getName());
        }
    }

    public void authenticate(String username) {
        authenticate(username, "");
        connector.addPlayer(this);
    }

    public void authenticate(String username, String password) {
        if (loggedIn) {
            connector.getLogger().severe(username + " is already logged in!");
            return;
        }

        loggedIn = true;
        // new thread so clients don't timeout
        new Thread(() -> {
            try {
                MinecraftProtocol protocol;
                if (password != null && !password.isEmpty()) {
                    protocol = new MinecraftProtocol(username, password);
                } else {
                    protocol = new MinecraftProtocol(username);
                }

                downstream = new Client(remoteServer.getAddress(), remoteServer.getPort(), protocol, new TcpSessionFactory());
                downstream.getSession().addListener(new SessionAdapter() {

                    @Override
                    public void connected(ConnectedEvent event) {
                        loggingIn = false;
                        loggedIn = true;
                        connector.getLogger().info(authenticationData.getName() + " (logged in as: " + protocol.getProfile().getName() + ")" + " has connected to remote java server on address " + remoteServer.getAddress());
                        playerEntity.setUuid(protocol.getProfile().getId());
                        playerEntity.setUsername(protocol.getProfile().getName());
                    }

                    @Override
                    public void disconnected(DisconnectedEvent event) {
                        loggingIn = false;
                        loggedIn = false;
                        connector.getLogger().info(authenticationData.getName() + " has disconnected from remote java server on address " + remoteServer.getAddress() + " because of " + event.getReason());
                        upstream.disconnect(event.getReason());
                    }

                    @Override
                    public void packetReceived(PacketReceivedEvent event) {
                        if (!closed)
                            Registry.JAVA.translate(event.getPacket().getClass(), event.getPacket(), GeyserSession.this);
                    }
                });

                downstream.getSession().connect();
            } catch (RequestException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    public void disconnect(String reason) {
        if (!closed) {
            loggedIn = false;
            if (downstream != null && downstream.getSession() != null) {
                downstream.getSession().disconnect(reason);
            }
            if (upstream != null && !upstream.isClosed()) {
                upstream.disconnect(reason);
            }
        }

        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        disconnect("Server closed.");
    }

    public void setAuthenticationData(AuthData authData) {
        authenticationData = authData;
    }

    @Override
    public String getName() {
        return authenticationData.getName();
    }

    @Override
    public void sendMessage(String message) {
        TextPacket textPacket = new TextPacket();
        textPacket.setPlatformChatId("");
        textPacket.setSourceName("");
        textPacket.setXuid("");
        textPacket.setType(TextPacket.Type.CHAT);
        textPacket.setNeedsTranslation(false);
        textPacket.setMessage(message);

        upstream.sendPacket(textPacket);
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String message : messages) {
            sendMessage(message);
        }
    }

    public void sendForm(FormWindow window, int id) {
        windowCache.showWindow(window, id);
    }

    @Override
    public InetSocketAddress getSocketAddress() {
        return this.upstream.getAddress();
    }

    public void sendForm(FormWindow window) {
        windowCache.showWindow(window);
    }

    private void startGame() {
        StartGamePacket startGamePacket = new StartGamePacket();
        startGamePacket.setUniqueEntityId(playerEntity.getGeyserId());
        startGamePacket.setRuntimeEntityId(playerEntity.getGeyserId());
        startGamePacket.setPlayerGamemode(0);
        startGamePacket.setPlayerPosition(new Vector3f(0, 69, 0));
        startGamePacket.setRotation(new Vector2f(1, 1));

        startGamePacket.setSeed(0);
        startGamePacket.setDimensionId(playerEntity.getDimension());
        startGamePacket.setGeneratorId(1);
        startGamePacket.setLevelGamemode(0);
        startGamePacket.setDifficulty(1);
        startGamePacket.setDefaultSpawn(new Vector3i(0, 0, 0));
        startGamePacket.setAcheivementsDisabled(true);
        startGamePacket.setTime(0);
        startGamePacket.setEduLevel(false);
        startGamePacket.setEduFeaturesEnabled(false);
        startGamePacket.setRainLevel(0);
        startGamePacket.setLightningLevel(0);
        startGamePacket.setMultiplayerGame(true);
        startGamePacket.setBroadcastingToLan(true);
        startGamePacket.getGamerules().add(new GameRule<>("showcoordinates", true));
        startGamePacket.setPlatformBroadcastMode(GamePublishSetting.PUBLIC);
        startGamePacket.setXblBroadcastMode(GamePublishSetting.PUBLIC);
        startGamePacket.setCommandsEnabled(true);
        startGamePacket.setTexturePacksRequired(false);
        startGamePacket.setBonusChestEnabled(false);
        startGamePacket.setStartingWithMap(false);
        startGamePacket.setTrustingPlayers(true);
        startGamePacket.setDefaultPlayerPermission(1);
        startGamePacket.setServerChunkTickRange(4);
        startGamePacket.setBehaviorPackLocked(false);
        startGamePacket.setResourcePackLocked(false);
        startGamePacket.setFromLockedWorldTemplate(false);
        startGamePacket.setUsingMsaGamertagsOnly(false);
        startGamePacket.setFromWorldTemplate(false);
        startGamePacket.setWorldTemplateOptionLocked(false);

        startGamePacket.setLevelId("world");
        startGamePacket.setWorldName("world");
        startGamePacket.setPremiumWorldTemplateId("00000000-0000-0000-0000-000000000000");
        startGamePacket.setCurrentTick(0);
        startGamePacket.setEnchantmentSeed(0);
        startGamePacket.setMultiplayerCorrelationId("");
        startGamePacket.setCachedPalette(Toolbox.CACHED_PALLETE);
        startGamePacket.setItemEntries(Toolbox.ITEMS);
        upstream.sendPacket(startGamePacket);

        PlayStatusPacket playStatusPacket = new PlayStatusPacket();
        playStatusPacket.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
        upstream.sendPacket(playStatusPacket);
    }
}
