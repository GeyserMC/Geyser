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

package org.geysermc.connector.network.session;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.InvalidCredentialsException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.window.VillagerTrade;
import com.github.steveice10.mc.protocol.packet.handshake.client.HandshakePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerRespawnPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import com.nukkitx.math.GenericMath;
import com.nukkitx.math.vector.*;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.data.*;
import com.nukkitx.protocol.bedrock.packet.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.common.AuthType;
import org.geysermc.common.window.FormWindow;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.remote.RemoteServer;
import org.geysermc.connector.network.session.auth.AuthData;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.connector.network.session.cache.*;
import org.geysermc.connector.network.translators.BiomeTranslator;
import org.geysermc.connector.network.translators.EntityIdentifierRegistry;
import org.geysermc.connector.network.translators.PacketTranslatorRegistry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.ChunkUtils;
import org.geysermc.connector.utils.LocaleUtils;
import org.geysermc.connector.utils.MathUtils;
import org.geysermc.connector.utils.SkinUtils;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.EncryptionUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class GeyserSession implements CommandSender {

    private final GeyserConnector connector;
    private final UpstreamSession upstream;
    private RemoteServer remoteServer;
    private Client downstream;
    @Setter
    private AuthData authData;
    @Setter
    private BedrockClientData clientData;

    private PlayerEntity playerEntity;
    private PlayerInventory inventory;

    private ChunkCache chunkCache;
    private EntityCache entityCache;
    private InventoryCache inventoryCache;
    private ScoreboardCache scoreboardCache;
    private WindowCache windowCache;
    @Setter
    private TeleportCache teleportCache;

    @Getter
    private final Long2ObjectMap<ClientboundMapItemDataPacket> storedMaps = new Long2ObjectOpenHashMap<>();

    /**
     * A map of Vector3i positions to Java entity IDs.
     * Used for translating Bedrock block actions to Java entity actions.
     */
    private final Object2LongMap<Vector3i> itemFrameCache = new Object2LongOpenHashMap<>();

    private DataCache<Packet> javaPacketCache;

    @Setter
    private Vector2i lastChunkPosition = null;
    private int renderDistance;

    private boolean loggedIn;
    private boolean loggingIn;

    @Setter
    private boolean spawned;
    private boolean closed;

    @Setter
    private GameMode gameMode = GameMode.SURVIVAL;

    private final AtomicInteger pendingDimSwitches = new AtomicInteger(0);

    @Setter
    private boolean sneaking;

    @Setter
    private boolean sprinting;

    @Setter
    private boolean jumping;

    @Setter
    private int breakingBlock;

    @Setter
    private Vector3i lastBlockPlacePosition;

    @Setter
    private String lastBlockPlacedId;

    @Setter
    private boolean interacting;

    @Setter
    private Vector3i lastInteractionPosition;

    @Setter
    private boolean switchingDimension = false;
    private boolean manyDimPackets = false;
    private ServerRespawnPacket lastDimPacket = null;

    @Setter
    private Entity ridingVehicleEntity;

    @Setter
    private int craftSlot = 0;

    @Setter
    private long lastWindowCloseTime = 0;

    @Setter
    private VillagerTrade[] villagerTrades;
    @Setter
    private long lastInteractedVillagerEid;

    /**
     * The current attack speed of the player. Used for sending proper cooldown timings.
     */
    @Setter
    private double attackSpeed;
    /**
     * The time of the last hit. Used to gauge how long the cooldown is taking.
     * This is a session variable in order to prevent more scheduled threads than necessary.
     */
    @Setter
    private long lastHitTime;

    private MinecraftProtocol protocol;

    public GeyserSession(GeyserConnector connector, BedrockServerSession bedrockServerSession) {
        this.connector = connector;
        this.upstream = new UpstreamSession(bedrockServerSession);

        this.chunkCache = new ChunkCache(this);
        this.entityCache = new EntityCache(this);
        this.inventoryCache = new InventoryCache(this);
        this.scoreboardCache = new ScoreboardCache(this);
        this.windowCache = new WindowCache(this);

        this.playerEntity = new PlayerEntity(new GameProfile(UUID.randomUUID(), "unknown"), 1, 1, Vector3f.ZERO, Vector3f.ZERO, Vector3f.ZERO);
        this.inventory = new PlayerInventory();

        this.javaPacketCache = new DataCache<>();

        this.spawned = false;
        this.loggedIn = false;

        this.inventoryCache.getInventories().put(0, inventory);
    }

    public void connect(RemoteServer remoteServer) {
        startGame();
        this.remoteServer = remoteServer;

        ChunkUtils.sendEmptyChunks(this, playerEntity.getPosition().toInt(), 0, false);

        BiomeDefinitionListPacket biomeDefinitionListPacket = new BiomeDefinitionListPacket();
        biomeDefinitionListPacket.setTag(BiomeTranslator.BIOMES);
        upstream.sendPacket(biomeDefinitionListPacket);

        AvailableEntityIdentifiersPacket entityPacket = new AvailableEntityIdentifiersPacket();
        entityPacket.setTag(EntityIdentifierRegistry.ENTITY_IDENTIFIERS);
        upstream.sendPacket(entityPacket);

        InventoryContentPacket creativePacket = new InventoryContentPacket();
        creativePacket.setContainerId(ContainerId.CREATIVE);
        creativePacket.setContents(ItemRegistry.CREATIVE_ITEMS);
        upstream.sendPacket(creativePacket);

        PlayStatusPacket playStatusPacket = new PlayStatusPacket();
        playStatusPacket.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
        upstream.sendPacket(playStatusPacket);

        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(getPlayerEntity().getGeyserId());
        List<Attribute> attributes = new ArrayList<>();
        // Default move speed
        // Bedrock clients move very fast by default until they get an attribute packet correcting the speed
        attributes.add(new Attribute("minecraft:movement", 0.0f, 1024f, 0.1f, 0.1f));
        attributesPacket.setAttributes(attributes);
        upstream.sendPacket(attributesPacket);
    }

    public void fetchOurSkin(PlayerListPacket.Entry entry) {
        PlayerSkinPacket playerSkinPacket = new PlayerSkinPacket();
        playerSkinPacket.setUuid(authData.getUUID());
        playerSkinPacket.setSkin(entry.getSkin());
        playerSkinPacket.setOldSkinName("OldName");
        playerSkinPacket.setNewSkinName("NewName");
        playerSkinPacket.setTrustedSkin(true);
        upstream.sendPacket(playerSkinPacket);
        getConnector().getLogger().debug("Sending skin for " + playerEntity.getUsername() + " " + authData.getUUID());
    }

    public void login() {
        if (connector.getAuthType() != AuthType.ONLINE) {
            connector.getLogger().info(
                    "Attempting to login using " + connector.getAuthType().name().toLowerCase() + " mode... " +
                            (connector.getAuthType() == AuthType.OFFLINE ?
                                    "authentication is disabled." : "authentication will be encrypted"
                            )
            );
            authenticate(authData.getName());
        }
    }

    public void authenticate(String username) {
        authenticate(username, "");
    }

    public void authenticate(String username, String password) {
        if (loggedIn) {
            connector.getLogger().severe(username + " is already logged in!");
            return;
        }

        loggingIn = true;
        // new thread so clients don't timeout
        new Thread(() -> {
            try {
                if (password != null && !password.isEmpty()) {
                    protocol = new MinecraftProtocol(username, password);
                } else {
                    protocol = new MinecraftProtocol(username);
                }

                boolean floodgate = connector.getAuthType() == AuthType.FLOODGATE;
                final PublicKey publicKey;

                if (floodgate) {
                    PublicKey key = null;
                    try {
                        key = EncryptionUtil.getKeyFromFile(
                                connector.getConfig().getFloodgateKeyFile(),
                                PublicKey.class
                        );
                    } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
                        connector.getLogger().error("Error while reading Floodgate key file", e);
                    }
                    publicKey = key;
                } else publicKey = null;

                if (publicKey != null) {
                    connector.getLogger().info("Loaded Floodgate key!");
                }

                downstream = new Client(remoteServer.getAddress(), remoteServer.getPort(), protocol, new TcpSessionFactory());
                downstream.getSession().addListener(new SessionAdapter() {
                    @Override
                    public void packetSending(PacketSendingEvent event) {
                        //todo move this somewhere else
                        if (event.getPacket() instanceof HandshakePacket && floodgate) {
                            String encrypted = "";
                            try {
                                encrypted = EncryptionUtil.encryptBedrockData(publicKey, new BedrockData(
                                        clientData.getGameVersion(),
                                        authData.getName(),
                                        authData.getXboxUUID(),
                                        clientData.getDeviceOS().ordinal(),
                                        clientData.getLanguageCode(),
                                        clientData.getCurrentInputMode().ordinal(),
                                        upstream.getSession().getAddress().getAddress().getHostAddress()
                                ));
                            } catch (Exception e) {
                                connector.getLogger().error("Failed to encrypt message", e);
                            }

                            HandshakePacket handshakePacket = event.getPacket();
                            event.setPacket(new HandshakePacket(
                                    handshakePacket.getProtocolVersion(),
                                    handshakePacket.getHostname() + '\0' + BedrockData.FLOODGATE_IDENTIFIER + '\0' + encrypted,
                                    handshakePacket.getPort(),
                                    handshakePacket.getIntent()
                            ));
                        }
                    }

                    @Override
                    public void connected(ConnectedEvent event) {
                        loggingIn = false;
                        loggedIn = true;
                        connector.getLogger().info(authData.getName() + " (logged in as: " + protocol.getProfile().getName() + ")" + " has connected to remote java server on address " + remoteServer.getAddress());
                        playerEntity.setUuid(protocol.getProfile().getId());
                        playerEntity.setUsername(protocol.getProfile().getName());

                        String locale = clientData.getLanguageCode();

                        // Let the user know there locale may take some time to download
                        // as it has to be extracted from a JAR
                        if (locale.toLowerCase().equals("en_us") && !LocaleUtils.LOCALE_MAPPINGS.containsKey("en_us")) {
                            sendMessage("Downloading your locale (en_us) this may take some time");
                        }

                        // Download and load the language for the player
                        LocaleUtils.downloadAndLoadLocale(locale);
                    }

                    @Override
                    public void disconnected(DisconnectedEvent event) {
                        loggingIn = false;
                        loggedIn = false;
                        connector.getLogger().info(authData.getName() + " has disconnected from remote java server on address " + remoteServer.getAddress() + " because of " + event.getReason());
                        if (event.getCause() != null) {
                            event.getCause().printStackTrace();
                        }
                        upstream.disconnect(event.getReason());
                    }

                    @Override
                    public void packetReceived(PacketReceivedEvent event) {
                        if (!closed) {
                            //handle consecutive respawn packets
                            if (event.getPacket().getClass().equals(ServerRespawnPacket.class)) {
                                manyDimPackets = lastDimPacket != null;
                                lastDimPacket = event.getPacket();
                                return;
                            } else if (lastDimPacket != null) {
                                PacketTranslatorRegistry.JAVA_TRANSLATOR.translate(lastDimPacket.getClass(), lastDimPacket, GeyserSession.this);
                                lastDimPacket = null;
                            }

                            // Required, or else Floodgate players break with Bukkit chunk caching
                            if (event.getPacket() instanceof LoginSuccessPacket) {
                                GameProfile profile = ((LoginSuccessPacket) event.getPacket()).getProfile();
                                playerEntity.setUsername(profile.getName());
                                playerEntity.setUuid(profile.getId());

                                // Check if they are not using a linked account
                                if (connector.getAuthType() == AuthType.OFFLINE || playerEntity.getUuid().getMostSignificantBits() == 0) {
                                    SkinUtils.handleBedrockSkin(playerEntity, clientData);
                                }
                            }

                            PacketTranslatorRegistry.JAVA_TRANSLATOR.translate(event.getPacket().getClass(), event.getPacket(), GeyserSession.this);
                        }
                    }

                    @Override
                    public void packetError(PacketErrorEvent event) {
                        connector.getLogger().warning("Downstream packet error! " + event.getCause().getMessage());
                        if (connector.getConfig().isDebugMode())
                            event.getCause().printStackTrace();
                        event.setSuppress(true);
                    }
                });

                downstream.getSession().connect();
                connector.addPlayer(this);
            } catch (InvalidCredentialsException | IllegalArgumentException e) {
                connector.getLogger().info("User '" + username + "' entered invalid login info, kicking.");
                disconnect("Invalid/incorrect login info");
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
                connector.getPlayers().remove(this.upstream.getAddress());
                upstream.disconnect(reason);
            }
        }

        this.chunkCache = null;
        this.entityCache = null;
        this.scoreboardCache = null;
        this.inventoryCache = null;
        this.windowCache = null;

        closed = true;
    }

    public void close() {
        disconnect("Server closed.");
    }

    public void setAuthenticationData(AuthData authData) {
        this.authData = authData;
    }

    @Override
    public String getName() {
        return authData.getName();
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
    public boolean isConsole() {
        return false;
    }

    public void sendForm(FormWindow window, int id) {
        windowCache.showWindow(window, id);
    }

    public void setRenderDistance(int renderDistance) {
        renderDistance = GenericMath.ceil(++renderDistance * MathUtils.SQRT_OF_TWO); //square to circle
        if (renderDistance > 32) renderDistance = 32; // <3 u ViaVersion but I don't like crashing clients x)
        this.renderDistance = renderDistance;

        ChunkRadiusUpdatedPacket chunkRadiusUpdatedPacket = new ChunkRadiusUpdatedPacket();
        chunkRadiusUpdatedPacket.setRadius(renderDistance);
        upstream.sendPacket(chunkRadiusUpdatedPacket);
    }

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
        startGamePacket.setPlayerPosition(Vector3f.from(0, 69, 0));
        startGamePacket.setRotation(Vector2f.from(1, 1));

        startGamePacket.setSeed(-1);
        startGamePacket.setDimensionId(playerEntity.getDimension());
        startGamePacket.setGeneratorId(1);
        startGamePacket.setLevelGamemode(0);
        startGamePacket.setDifficulty(1);
        startGamePacket.setDefaultSpawn(Vector3i.ZERO);
        startGamePacket.setAchievementsDisabled(true);
        startGamePacket.setTime(-1);
        startGamePacket.setEduEditionOffers(0);
        startGamePacket.setEduFeaturesEnabled(false);
        startGamePacket.setRainLevel(0);
        startGamePacket.setLightningLevel(0);
        startGamePacket.setMultiplayerGame(true);
        startGamePacket.setBroadcastingToLan(true);
        startGamePacket.getGamerules().add(new GameRuleData<>("showcoordinates", true));
        startGamePacket.setPlatformBroadcastMode(GamePublishSetting.PUBLIC);
        startGamePacket.setXblBroadcastMode(GamePublishSetting.PUBLIC);
        startGamePacket.setCommandsEnabled(true);
        startGamePacket.setTexturePacksRequired(false);
        startGamePacket.setBonusChestEnabled(false);
        startGamePacket.setStartingWithMap(false);
        startGamePacket.setTrustingPlayers(true);
        startGamePacket.setDefaultPlayerPermission(PlayerPermission.MEMBER);
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
        // startGamePacket.setCurrentTick(0);
        startGamePacket.setEnchantmentSeed(0);
        startGamePacket.setMultiplayerCorrelationId("");
        startGamePacket.setBlockPalette(BlockTranslator.BLOCKS);
        startGamePacket.setItemEntries(ItemRegistry.ITEMS);
        startGamePacket.setVanillaVersion("*");
        // startGamePacket.setMovementServerAuthoritative(true);
        upstream.sendPacket(startGamePacket);
    }

    public boolean confirmTeleport(Vector3d position) {
        if (teleportCache != null) {
            if (!teleportCache.canConfirm(position)) {
                GeyserConnector.getInstance().getLogger().debug("Unconfirmed Teleport " + teleportCache.getTeleportConfirmId()
                        + " Ignore movement " + position + " expected " + teleportCache);
                return false;
            }
            int teleportId = teleportCache.getTeleportConfirmId();
            teleportCache = null;
            ClientTeleportConfirmPacket teleportConfirmPacket = new ClientTeleportConfirmPacket(teleportId);
            sendDownstreamPacket(teleportConfirmPacket);
        }
        return true;
    }

    /**
     * Queue a packet to be sent to player.
     *
     * @param packet the bedrock packet from the NukkitX protocol lib
     */
    public void sendUpstreamPacket(BedrockPacket packet) {
        if (upstream != null && !upstream.isClosed()) {
            upstream.sendPacket(packet);
        } else {
            connector.getLogger().debug("Tried to send upstream packet " + packet.getClass().getSimpleName() + " but the session was null");
        }
    }

    /**
     * Send a packet immediately to the player.
     * 
     * @param packet the bedrock packet from the NukkitX protocol lib
     */
    public void sendUpstreamPacketImmediately(BedrockPacket packet) {
        if (upstream != null && !upstream.isClosed()) {
            upstream.sendPacketImmediately(packet);
        } else {
            connector.getLogger().debug("Tried to send upstream packet " + packet.getClass().getSimpleName() + " immediately but the session was null");
        }
    }

    /**
     * Send a packet to the remote server.
     *
     * @param packet the java edition packet from MCProtocolLib
     */
    public void sendDownstreamPacket(Packet packet) {
        if (downstream != null && downstream.getSession() != null && protocol.getSubProtocol().equals(SubProtocol.GAME)) {
            downstream.getSession().send(packet);
        } else {
            connector.getLogger().debug("Tried to send downstream packet " + packet.getClass().getSimpleName() + " before connected to the server");
        }
    }
}
