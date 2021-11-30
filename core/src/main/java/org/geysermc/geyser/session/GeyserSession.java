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

package org.geysermc.geyser.session;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.AuthPendingException;
import com.github.steveice10.mc.auth.exception.request.InvalidCredentialsException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.UnexpectedEncryptionException;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Pose;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.recipe.Recipe;
import com.github.steveice10.mc.protocol.data.game.statistic.Statistic;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundCustomQueryPacket;
import com.github.steveice10.packetlib.BuiltinFlags;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import com.github.steveice10.packetlib.tcp.TcpSession;
import com.nukkitx.math.GenericMath;
import com.nukkitx.math.vector.*;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.data.*;
import com.nukkitx.protocol.bedrock.data.command.CommandPermission;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.v471.Bedrock_v471;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.geysermc.common.PlatformType;
import org.geysermc.cumulus.Form;
import org.geysermc.cumulus.util.FormBuilder;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.command.CommandSender;
import org.geysermc.geyser.configuration.EmoteOffhandWorkaroundOption;
import org.geysermc.geyser.entity.InteractiveTagManager;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.entity.type.player.SkullPlayerEntity;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.network.netty.LocalSession;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.auth.AuthData;
import org.geysermc.geyser.session.auth.AuthType;
import org.geysermc.geyser.session.auth.BedrockClientData;
import org.geysermc.geyser.session.cache.*;
import org.geysermc.geyser.skin.FloodgateSkinUploader;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.geyser.util.LoginEncryptionUtils;
import org.geysermc.geyser.util.MathUtils;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class GeyserSession implements GeyserConnection, CommandSender {

    private final GeyserImpl geyser;
    private final UpstreamSession upstream;
    /**
     * The loop where all packets and ticking is processed to prevent concurrency issues.
     * If this is manually called, ensure that any exceptions are properly handled.
     */
    private final EventLoop eventLoop;
    private TcpSession downstream;
    @Setter
    private AuthData authData;
    @Setter
    private BedrockClientData clientData;

    /* Setter for GeyserConnect */
    @Setter
    private String remoteAddress;
    @Setter
    private int remotePort;
    @Setter
    private AuthType remoteAuthType;
    /* Setter for GeyserConnect */

    @Deprecated
    @Setter
    private boolean microsoftAccount;

    private final SessionPlayerEntity playerEntity;

    private final AdvancementsCache advancementsCache;
    private final BookEditCache bookEditCache;
    private final ChunkCache chunkCache;
    private final EntityCache entityCache;
    private final EntityEffectCache effectCache;
    private final FormCache formCache;
    private final LodestoneCache lodestoneCache;
    private final PistonCache pistonCache;
    private final PreferencesCache preferencesCache;
    private final TagCache tagCache;
    private final WorldCache worldCache;

    private final Int2ObjectMap<TeleportCache> teleportMap = new Int2ObjectOpenHashMap<>();

    private final WorldBorder worldBorder;
    /**
     * Whether simulated fog has been sent to the client or not.
     */
    private boolean isInWorldBorderWarningArea = false;

    private final PlayerInventory playerInventory;
    @Setter
    private Inventory openInventory;
    @Setter
    private boolean closingInventory;

    @Setter
    private InventoryTranslator inventoryTranslator = InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR;

    /**
     * Use {@link #getNextItemNetId()} instead for consistency
     */
    @Getter(AccessLevel.NONE)
    private final AtomicInteger itemNetId = new AtomicInteger(2);

    @Setter
    private ScheduledFuture<?> craftingGridFuture;

    /**
     * Stores session collision
     */
    private final CollisionManager collisionManager;

    /**
     * Stores the block mappings for this specific version.
     */
    @Setter
    private BlockMappings blockMappings;

    /**
     * Stores the item translations for this specific version.
     */
    @Setter
    private ItemMappings itemMappings;

    private final Map<Vector3i, SkullPlayerEntity> skullCache = new Object2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ClientboundMapItemDataPacket> storedMaps = new Long2ObjectOpenHashMap<>();

    /**
     * Required to decode biomes correctly.
     */
    @Setter
    private int biomeGlobalPalette;
    /**
     * Stores the map between Java and Bedrock biome network IDs.
     */
    private final Int2IntMap biomeTranslations = new Int2IntOpenHashMap();

    /**
     * A map of Vector3i positions to Java entities.
     * Used for translating Bedrock block actions to Java entity actions.
     */
    private final Map<Vector3i, ItemFrameEntity> itemFrameCache = new Object2ObjectOpenHashMap<>();

    /**
     * Stores a list of all lectern locations and their block entity tags.
     * See {@link WorldManager#getLecternDataAt(GeyserSession, int, int, int, boolean)}
     * for more information.
     */
    private final Set<Vector3i> lecternCache;

    /**
     * A list of all players that have a player head on with a custom texture.
     * Our workaround for these players is to give them a custom skin and geometry to emulate wearing a custom skull.
     */
    private final Set<UUID> playerWithCustomHeads = new ObjectOpenHashSet<>();

    @Setter
    private boolean droppingLecternBook;

    @Setter
    private Vector2i lastChunkPosition = null;
    private int renderDistance;

    // Exposed for GeyserConnect usage
    protected boolean sentSpawnPacket;

    private boolean loggedIn;
    private boolean loggingIn;

    @Setter
    private boolean spawned;
    /**
     * Accessed on the initial Java and Bedrock packet processing threads
     */
    private volatile boolean closed;

    @Setter
    private GameMode gameMode = GameMode.SURVIVAL;

    /**
     * Keeps track of the world name for respawning.
     */
    @Setter
    private String worldName = null;

    private boolean sneaking;

    /**
     * Stores the Java pose that the server and/or Geyser believes the player currently has.
     */
    @Setter
    private Pose pose = Pose.STANDING;

    @Setter
    private boolean sprinting;

    /**
     * Whether the player is swimming in water.
     * Used to update speed when crawling.
     */
    @Setter
    private boolean swimmingInWater;

    /**
     * Tracks the original speed attribute.
     *
     * We need to do this in order to emulate speeds when sneaking under 1.5-blocks-tall areas if the player isn't sneaking,
     * and when crawling.
     */
    @Setter
    private float originalSpeedAttribute;

    /**
     * The dimension of the player.
     * As all entities are in the same world, this can be safely applied to all other entities.
     */
    @Setter
    private String dimension = DimensionUtils.OVERWORLD;

    @Setter
    private int breakingBlock;

    @Setter
    private Vector3i lastBlockPlacePosition;

    @Setter
    private String lastBlockPlacedId;

    @Setter
    private boolean interacting;

    /**
     * Stores the last position of the block the player interacted with. This can either be a block that the client
     * placed or an existing block the player interacted with (for example, a chest). <br>
     * Initialized as (0, 0, 0) so it is always not-null.
     */
    @Setter
    private Vector3i lastInteractionBlockPosition = Vector3i.ZERO;

    /**
     * Stores the position of the player the last time they interacted.
     * Used to verify that the player did not move since their last interaction. <br>
     * Initialized as (0, 0, 0) so it is always not-null.
     */
    @Setter
    private Vector3f lastInteractionPlayerPosition = Vector3f.ZERO;

    @Setter
    private Entity ridingVehicleEntity;

    /**
     * The entity that the client is currently looking at.
     */
    @Setter
    private Entity mouseoverEntity;

    @Setter
    private Int2ObjectMap<Recipe> craftingRecipes;
    private final Set<String> unlockedRecipes;
    private final AtomicInteger lastRecipeNetId;

    /**
     * Saves a list of all stonecutter recipes, for use in a stonecutter inventory.
     * The key is the Java ID of the item; the values are all the possible outputs' Java IDs sorted by their string identifier
     */
    @Setter
    private Int2ObjectMap<IntList> stonecutterRecipes;

    /**
     * The current attack speed of the player. Used for sending proper cooldown timings.
     * Setting a default fixes cooldowns not showing up on a fresh world.
     */
    @Setter
    private double attackSpeed = 4.0d;
    /**
     * The time of the last hit. Used to gauge how long the cooldown is taking.
     * This is a session variable in order to prevent more scheduled threads than necessary.
     */
    @Setter
    private long lastHitTime;

    /**
     * Saves if the client is steering left on a boat.
     */
    @Setter
    private boolean steeringLeft;
    /**
     * Saves if the client is steering right on a boat.
     */
    @Setter
    private boolean steeringRight;

    /**
     * Store the last time the player interacted. Used to fix a right-click spam bug.
     * See https://github.com/GeyserMC/Geyser/issues/503 for context.
     */
    @Setter
    private long lastInteractionTime;

    /**
     * Stores a future interaction to place a bucket. Will be cancelled if the client instead intended to
     * interact with a block.
     */
    @Setter
    private ScheduledFuture<?> bucketScheduledFuture;

    /**
     * Used to send a movement packet every three seconds if the player hasn't moved. Prevents timeouts when AFK in certain instances.
     */
    @Setter
    private long lastMovementTimestamp = System.currentTimeMillis();

    /**
     * Used to send a ServerboundMoveVehiclePacket for every PlayerInputPacket after idling on a boat/horse for more than 100ms
     */
    @Setter
    private long lastVehicleMoveTimestamp = System.currentTimeMillis();

    /**
     * Controls whether the daylight cycle gamerule has been sent to the client, so the sun/moon remain motionless.
     */
    private boolean daylightCycle = true;

    private boolean reducedDebugInfo = false;

    /**
     * The op permission level set by the server
     */
    @Setter
    private int opPermissionLevel = 0;

    /**
     * If the current player can fly
     */
    @Setter
    private boolean canFly = false;

    /**
     * If the current player is flying
     */
    private boolean flying = false;

    /**
     * Caches current rain status.
     */
    @Setter
    private boolean raining = false;

    /**
     * Caches current thunder status.
     */
    @Setter
    private boolean thunder = false;

    /**
     * Stores the last text inputted into a sign.
     * <p>
     * Bedrock sends packets every time you update the sign, Java only wants the final packet.
     * Until we determine that the user has finished editing, we save the sign's current status.
     */
    @Setter
    private String lastSignMessage;

    /**
     * Stores a map of all statistics sent from the server.
     * The server only sends new statistics back to us, so in order to show all statistics we need to cache existing ones.
     */
    private final Map<Statistic, Integer> statistics = new HashMap<>();

    /**
     * Whether we're expecting statistics to be sent back to us.
     */
    @Setter
    private boolean waitingForStatistics = false;

    private final Set<UUID> emotes;

    /**
     * Whether advanced tooltips will be added to the player's items.
     */
    @Setter
    private boolean advancedTooltips = false;

    /**
     * The thread that will run every 50 milliseconds - one Minecraft tick.
     */
    private ScheduledFuture<?> tickThread = null;

    private MinecraftProtocol protocol;

    public GeyserSession(GeyserImpl geyser, BedrockServerSession bedrockServerSession, EventLoop eventLoop) {
        this.geyser = geyser;
        this.upstream = new UpstreamSession(bedrockServerSession);
        this.eventLoop = eventLoop;

        this.advancementsCache = new AdvancementsCache(this);
        this.bookEditCache = new BookEditCache(this);
        this.chunkCache = new ChunkCache(this);
        this.entityCache = new EntityCache(this);
        this.effectCache = new EntityEffectCache();
        this.formCache = new FormCache(this);
        this.lodestoneCache = new LodestoneCache();
        this.pistonCache = new PistonCache(this);
        this.preferencesCache = new PreferencesCache(this);
        this.tagCache = new TagCache();
        this.worldCache = new WorldCache(this);

        this.worldBorder = new WorldBorder(this);

        this.collisionManager = new CollisionManager(this);

        this.playerEntity = new SessionPlayerEntity(this);
        collisionManager.updatePlayerBoundingBox(this.playerEntity.getPosition());

        this.playerInventory = new PlayerInventory();
        this.openInventory = null;
        this.craftingRecipes = new Int2ObjectOpenHashMap<>();
        this.unlockedRecipes = new ObjectOpenHashSet<>();
        this.lastRecipeNetId = new AtomicInteger(1);

        this.spawned = false;
        this.loggedIn = false;

        if (geyser.getWorldManager().shouldExpectLecternHandled()) {
            // Unneeded on these platforms
            this.lecternCache = null;
        } else {
            this.lecternCache = new ObjectOpenHashSet<>();
        }

        if (geyser.getConfig().getEmoteOffhandWorkaround() != EmoteOffhandWorkaroundOption.NO_EMOTES) {
            this.emotes = new HashSet<>();
            geyser.getSessionManager().getSessions().values().forEach(player -> this.emotes.addAll(player.getEmotes()));
        } else {
            this.emotes = null;
        }

        bedrockServerSession.addDisconnectHandler(disconnectReason -> {
            InetAddress address = bedrockServerSession.getRealAddress().getAddress();
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.disconnect", address, disconnectReason));

            disconnect(disconnectReason.name());
            geyser.getSessionManager().removeSession(this);
        });

        this.remoteAddress = geyser.getConfig().getRemote().getAddress();
        this.remotePort = geyser.getConfig().getRemote().getPort();
        this.remoteAuthType = geyser.getConfig().getRemote().getAuthType();
    }

    /**
     * Send all necessary packets to load Bedrock into the server
     */
    public void connect() {
        startGame();
        sentSpawnPacket = true;

        // Set the hardcoded shield ID to the ID we just defined in StartGamePacket
        upstream.getSession().getHardcodedBlockingId().set(this.itemMappings.getStoredItems().shield().getBedrockId());

        if (this.itemMappings.getFurnaceMinecartData() != null) {
            ItemComponentPacket componentPacket = new ItemComponentPacket();
            componentPacket.getItems().add(this.itemMappings.getFurnaceMinecartData());
            upstream.sendPacket(componentPacket);
        }

        ChunkUtils.sendEmptyChunks(this, playerEntity.getPosition().toInt(), 0, false);

        BiomeDefinitionListPacket biomeDefinitionListPacket = new BiomeDefinitionListPacket();
        biomeDefinitionListPacket.setDefinitions(Registries.BIOMES_NBT.get());
        upstream.sendPacket(biomeDefinitionListPacket);

        AvailableEntityIdentifiersPacket entityPacket = new AvailableEntityIdentifiersPacket();
        entityPacket.setIdentifiers(Registries.BEDROCK_ENTITY_IDENTIFIERS.get());
        upstream.sendPacket(entityPacket);

        CreativeContentPacket creativePacket = new CreativeContentPacket();
        creativePacket.setContents(this.itemMappings.getCreativeItems());
        upstream.sendPacket(creativePacket);

        PlayStatusPacket playStatusPacket = new PlayStatusPacket();
        playStatusPacket.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
        upstream.sendPacket(playStatusPacket);

        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(getPlayerEntity().getGeyserId());
        // Default move speed
        // Bedrock clients move very fast by default until they get an attribute packet correcting the speed
        attributesPacket.setAttributes(Collections.singletonList(
                new AttributeData("minecraft:movement", 0.0f, 1024f, 0.1f, 0.1f)));
        upstream.sendPacket(attributesPacket);

        GameRulesChangedPacket gamerulePacket = new GameRulesChangedPacket();
        // Only allow the server to send health information
        // Setting this to false allows natural regeneration to work false but doesn't break it being true
        gamerulePacket.getGameRules().add(new GameRuleData<>("naturalregeneration", false));
        // Don't let the client modify the inventory on death
        // Setting this to true allows keep inventory to work if enabled but doesn't break functionality being false
        gamerulePacket.getGameRules().add(new GameRuleData<>("keepinventory", true));
        // Ensure client doesn't try and do anything funky; the server handles this for us
        gamerulePacket.getGameRules().add(new GameRuleData<>("spawnradius", 0));
        upstream.sendPacket(gamerulePacket);
    }

    public void login() {
        if (this.remoteAuthType != AuthType.ONLINE) {
            if (this.remoteAuthType == AuthType.OFFLINE) {
                geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.auth.login.offline"));
            } else {
                geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.auth.login.floodgate"));
            }
            authenticate(authData.name());
        }
    }

    public void authenticate(String username) {
        authenticate(username, "");
    }

    public void authenticate(String username, String password) {
        if (loggedIn) {
            geyser.getLogger().severe(GeyserLocale.getLocaleStringLog("geyser.auth.already_loggedin", username));
            return;
        }

        loggingIn = true;

        // Use a future to prevent timeouts as all the authentication is handled sync
        // This will be changed with the new protocol library.
        CompletableFuture.supplyAsync(() -> {
            try {
                if (password != null && !password.isEmpty()) {
                    AuthenticationService authenticationService;
                    if (microsoftAccount) {
                        authenticationService = new MsaAuthenticationService(GeyserImpl.OAUTH_CLIENT_ID);
                    } else {
                        authenticationService = new MojangAuthenticationService();
                    }
                    authenticationService.setUsername(username);
                    authenticationService.setPassword(password);
                    authenticationService.login();

                    GameProfile profile = authenticationService.getSelectedProfile();
                    if (profile == null) {
                        // Java account is offline
                        disconnect(GeyserLocale.getPlayerLocaleString("geyser.network.remote.invalid_account", clientData.getLanguageCode()));
                        return null;
                    }

                    protocol = new MinecraftProtocol(profile, authenticationService.getAccessToken());
                } else {
                    // always replace spaces when using Floodgate,
                    // as usernames with spaces cause issues with Bungeecord's login cycle.
                    // However, this doesn't affect the final username as Floodgate is still in charge of that.
                    // So if you have (for example) replace spaces enabled on Floodgate the spaces will re-appear.
                    String validUsername = username;
                    if (remoteAuthType == AuthType.FLOODGATE) {
                        validUsername = username.replace(' ', '_');
                    }

                    protocol = new MinecraftProtocol(validUsername);
                }
            } catch (InvalidCredentialsException | IllegalArgumentException e) {
                geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.auth.login.invalid", username));
                disconnect(GeyserLocale.getPlayerLocaleString("geyser.auth.login.invalid.kick", getClientData().getLanguageCode()));
            } catch (RequestException ex) {
                disconnect(ex.getMessage());
            }
            return null;
        }).whenComplete((aVoid, ex) -> {
            if (ex != null) {
                disconnect(ex.toString());
            }
            if (this.closed) {
                if (ex != null) {
                    geyser.getLogger().error("", ex);
                }
                // Client disconnected during the authentication attempt
                return;
            }

            connectDownstream();
        });
    }

    /**
     * Present a form window to the user asking to log in with another web browser
     */
    public void authenticateWithMicrosoftCode() {
        if (loggedIn) {
            geyser.getLogger().severe(GeyserLocale.getLocaleStringLog("geyser.auth.already_loggedin", getAuthData().name()));
            return;
        }

        loggingIn = true;

        // This just looks cool
        SetTimePacket packet = new SetTimePacket();
        packet.setTime(16000);
        sendUpstreamPacket(packet);

        // new thread so clients don't timeout
        MsaAuthenticationService msaAuthenticationService = new MsaAuthenticationService(GeyserImpl.OAUTH_CLIENT_ID);

        // Use a future to prevent timeouts as all the authentication is handled sync
        // This will be changed with the new protocol library.
        CompletableFuture.supplyAsync(() -> {
            try {
                return msaAuthenticationService.getAuthCode();
            } catch (RequestException e) {
                throw new CompletionException(e);
            }
        }).whenComplete((response, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
                disconnect(ex.toString());
                return;
            }
            LoginEncryptionUtils.buildAndShowMicrosoftCodeWindow(this, response);
            attemptCodeAuthentication(msaAuthenticationService);
        });
    }

    /**
     * Poll every second to see if the user has successfully signed in
     */
    private void attemptCodeAuthentication(MsaAuthenticationService msaAuthenticationService) {
        if (loggedIn || closed) {
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            try {
                msaAuthenticationService.login();
                GameProfile profile = msaAuthenticationService.getSelectedProfile();
                if (profile == null) {
                    // Java account is offline
                    disconnect(GeyserLocale.getPlayerLocaleString("geyser.network.remote.invalid_account", clientData.getLanguageCode()));
                    return null;
                }

                return new MinecraftProtocol(profile, msaAuthenticationService.getAccessToken());
            } catch (RequestException e) {
                throw new CompletionException(e);
            }
        }).whenComplete((response, ex) -> {
            if (ex != null) {
                if (!(ex instanceof CompletionException completionException) || !(completionException.getCause() instanceof AuthPendingException)) {
                    geyser.getLogger().error("Failed to log in with Microsoft code!", ex);
                    disconnect(ex.toString());
                } else {
                    // Wait one second before trying again
                    geyser.getScheduledThread().schedule(() -> attemptCodeAuthentication(msaAuthenticationService), 1, TimeUnit.SECONDS);
                }
                return;
            }
            if (!closed) {
                this.protocol = response;
                connectDownstream();
            }
        });
    }

    /**
     * After getting whatever credentials needed, we attempt to join the Java server.
     */
    private void connectDownstream() {
        boolean floodgate = this.remoteAuthType == AuthType.FLOODGATE;

        // Start ticking
        tickThread = eventLoop.scheduleAtFixedRate(this::tick, 50, 50, TimeUnit.MILLISECONDS);

        if (geyser.getBootstrap().getSocketAddress() != null) {
            // We're going to connect through the JVM and not through TCP
            downstream = new LocalSession(this.remoteAddress, this.remotePort,
                    geyser.getBootstrap().getSocketAddress(), upstream.getAddress().getAddress().getHostAddress(), this.protocol);
        } else {
            downstream = new TcpClientSession(this.remoteAddress, this.remotePort, this.protocol);
            disableSrvResolving();
        }

        if (geyser.getConfig().getRemote().isUseProxyProtocol()) {
            downstream.setFlag(BuiltinFlags.ENABLE_CLIENT_PROXY_PROTOCOL, true);
            downstream.setFlag(BuiltinFlags.CLIENT_PROXIED_ADDRESS, upstream.getAddress());
        }
        if (geyser.getConfig().isForwardPlayerPing()) {
            // Let Geyser handle sending the keep alive
            downstream.setFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, false);
        }
        downstream.addListener(new SessionAdapter() {
            @Override
            public void packetSending(PacketSendingEvent event) {
                //todo move this somewhere else
                if (event.getPacket() instanceof ClientIntentionPacket) {
                    String addressSuffix;
                    if (floodgate) {
                        byte[] encryptedData;

                        try {
                            FloodgateSkinUploader skinUploader = geyser.getSkinUploader();
                            FloodgateCipher cipher = geyser.getCipher();

                            encryptedData = cipher.encryptFromString(BedrockData.of(
                                    clientData.getGameVersion(),
                                    authData.name(),
                                    authData.xuid(),
                                    clientData.getDeviceOs().ordinal(),
                                    clientData.getLanguageCode(),
                                    clientData.getUiProfile().ordinal(),
                                    clientData.getCurrentInputMode().ordinal(),
                                    upstream.getAddress().getAddress().getHostAddress(),
                                    skinUploader.getId(),
                                    skinUploader.getVerifyCode()
                            ).toString());
                        } catch (Exception e) {
                            geyser.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.auth.floodgate.encrypt_fail"), e);
                            disconnect(GeyserLocale.getPlayerLocaleString("geyser.auth.floodgate.encryption_fail", getClientData().getLanguageCode()));
                            return;
                        }

                        addressSuffix = '\0' + new String(encryptedData, StandardCharsets.UTF_8);
                    } else {
                        addressSuffix = "";
                    }

                    ClientIntentionPacket intentionPacket = event.getPacket();

                    String address;
                    if (geyser.getConfig().getRemote().isForwardHost()) {
                        address = clientData.getServerAddress().split(":")[0];
                    } else {
                        address = intentionPacket.getHostname();
                    }

                    event.setPacket(intentionPacket.withHostname(address + addressSuffix));
                }
            }

            @Override
            public void connected(ConnectedEvent event) {
                loggingIn = false;
                loggedIn = true;

                if (downstream instanceof LocalSession) {
                    // Connected directly to the server
                    geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.remote.connect_internal",
                            authData.name(), protocol.getProfile().getName()));
                } else {
                    // Connected to an IP address
                    geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.remote.connect",
                            authData.name(), protocol.getProfile().getName(), remoteAddress));
                }

                UUID uuid = protocol.getProfile().getId();
                if (uuid == null) {
                    // Set what our UUID *probably* is going to be
                    if (remoteAuthType == AuthType.FLOODGATE) {
                        uuid = new UUID(0, Long.parseLong(authData.xuid()));
                    } else {
                        uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + protocol.getProfile().getName()).getBytes(StandardCharsets.UTF_8));
                    }
                }
                playerEntity.setUuid(uuid);
                playerEntity.setUsername(protocol.getProfile().getName());

                String locale = clientData.getLanguageCode();

                // Let the user know there locale may take some time to download
                // as it has to be extracted from a JAR
                if (locale.equalsIgnoreCase("en_us") && !MinecraftLocale.LOCALE_MAPPINGS.containsKey("en_us")) {
                    // This should probably be left hardcoded as it will only show for en_us clients
                    sendMessage("Loading your locale (en_us); if this isn't already downloaded, this may take some time");
                }

                // Download and load the language for the player
                MinecraftLocale.downloadAndLoadLocale(locale);
            }

            @Override
            public void disconnected(DisconnectedEvent event) {
                loggingIn = false;
                loggedIn = false;

                String disconnectMessage;
                Throwable cause = event.getCause();
                if (cause instanceof UnexpectedEncryptionException) {
                    if (remoteAuthType != AuthType.FLOODGATE) {
                        // Server expects online mode
                        disconnectMessage = GeyserLocale.getPlayerLocaleString("geyser.network.remote.authentication_type_mismatch", getLocale());
                        // Explain that they may be looking for Floodgate.
                        geyser.getLogger().warning(GeyserLocale.getLocaleStringLog(
                                geyser.getPlatformType() == PlatformType.STANDALONE ?
                                        "geyser.network.remote.floodgate_explanation_standalone"
                                        : "geyser.network.remote.floodgate_explanation_plugin",
                                Constants.FLOODGATE_DOWNLOAD_LOCATION
                        ));
                    } else {
                        // Likely that Floodgate is not configured correctly.
                        disconnectMessage = GeyserLocale.getPlayerLocaleString("geyser.network.remote.floodgate_login_error", getLocale());
                        if (geyser.getPlatformType() == PlatformType.STANDALONE) {
                            geyser.getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.network.remote.floodgate_login_error_standalone"));
                        }
                    }
                } else if (cause instanceof ConnectException) {
                    // Server is offline, probably
                    disconnectMessage = GeyserLocale.getPlayerLocaleString("geyser.network.remote.server_offline", getLocale());
                } else {
                    disconnectMessage = MessageTranslator.convertMessageLenient(event.getReason());
                }

                if (downstream instanceof LocalSession) {
                    geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.remote.disconnect_internal", authData.name(), disconnectMessage));
                } else {
                    geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.remote.disconnect", authData.name(), remoteAddress, disconnectMessage));
                }
                if (cause != null) {
                    cause.printStackTrace();
                }

                upstream.disconnect(disconnectMessage);
            }

            @Override
            public void packetReceived(Session session, Packet packet) {
                Registries.JAVA_PACKET_TRANSLATORS.translate(packet.getClass(), packet, GeyserSession.this);
            }

            @Override
            public void packetError(PacketErrorEvent event) {
                geyser.getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.network.downstream_error", event.getCause().getMessage()));
                if (geyser.getConfig().isDebugMode())
                    event.getCause().printStackTrace();
                event.setSuppress(true);
            }
        });

        if (!daylightCycle) {
            setDaylightCycle(true);
        }

        downstream.connect();
    }

    public void disconnect(String reason) {
        if (!closed) {
            loggedIn = false;
            if (downstream != null) {
                downstream.disconnect(reason);
            }
            if (upstream != null && !upstream.isClosed()) {
                geyser.getSessionManager().removeSession(this);
                upstream.disconnect(reason);
            }
        }

        if (tickThread != null) {
            tickThread.cancel(false);
        }

        closed = true;
    }

    public void close() {
        disconnect(GeyserLocale.getPlayerLocaleString("geyser.network.close", getClientData().getLanguageCode()));
    }

    /**
     * Executes a task and prints a stack trace if an error occurs.
     */
    public void executeInEventLoop(Runnable runnable) {
        eventLoop.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                geyser.getLogger().error("Error thrown in " + this.name() + "'s event loop!", e);
            }
        });
    }

    /**
     * Schedules a task and prints a stack trace if an error occurs.
     */
    public ScheduledFuture<?> scheduleInEventLoop(Runnable runnable, long duration, TimeUnit timeUnit) {
        return eventLoop.schedule(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                geyser.getLogger().error("Error thrown in " + this.name() + "'s event loop!", e);
            }
        }, duration, timeUnit);
    }

    /**
     * Called every 50 milliseconds - one Minecraft tick.
     */
    protected void tick() {
        try {
            pistonCache.tick();
            // Check to see if the player's position needs updating - a position update should be sent once every 3 seconds
            if (spawned && (System.currentTimeMillis() - lastMovementTimestamp) > 3000) {
                // Recalculate in case something else changed position
                Vector3d position = collisionManager.adjustBedrockPosition(playerEntity.getPosition(), playerEntity.isOnGround(), false);
                // A null return value cancels the packet
                if (position != null) {
                    ServerboundMovePlayerPosPacket packet = new ServerboundMovePlayerPosPacket(playerEntity.isOnGround(),
                            position.getX(), position.getY(), position.getZ());
                    sendDownstreamPacket(packet);
                }
                lastMovementTimestamp = System.currentTimeMillis();
            }

            if (worldBorder.isResizing()) {
                worldBorder.resize();
            }

            if (!worldBorder.isWithinWarningBoundaries()) {
                // Show particles representing where the world border is
                worldBorder.drawWall();
                // Set the mood
                if (!isInWorldBorderWarningArea) {
                    isInWorldBorderWarningArea = true;
                    WorldBorder.sendFog(this, "minecraft:fog_crimson_forest");
                }
            } else if (isInWorldBorderWarningArea) {
                // Clear fog as we are outside the world border now
                WorldBorder.removeFog(this);
                isInWorldBorderWarningArea = false;
            }


            for (Tickable entity : entityCache.getTickableEntities()) {
                entity.tick();
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void setAuthenticationData(AuthData authData) {
        this.authData = authData;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;

        // Update pose and bounding box on our end
        AttributeData speedAttribute;
        if (!sneaking && (speedAttribute = adjustSpeed()) != null) {
            // Update attributes since we're still "sneaking" under a 1.5-block-tall area
            UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
            attributesPacket.setRuntimeEntityId(playerEntity.getGeyserId());
            attributesPacket.setAttributes(Collections.singletonList(speedAttribute));
            sendUpstreamPacket(attributesPacket);
            // the server *should* update our pose once it has returned to normal
        } else {
            if (!flying) {
                // The pose and bounding box should not be updated if the player is flying
                setSneakingPose(sneaking);
            }
            collisionManager.updateScaffoldingFlags(false);
        }

        playerEntity.updateBedrockMetadata();

        if (mouseoverEntity != null) {
            // Horses, etc can change their property depending on if you're sneaking
            InteractiveTagManager.updateTag(this, mouseoverEntity);
        }
    }

    private void setSneakingPose(boolean sneaking) {
        this.pose = sneaking ? Pose.SNEAKING : Pose.STANDING;
        playerEntity.getDirtyMetadata().put(EntityData.BOUNDING_BOX_HEIGHT, sneaking ? 1.5f : playerEntity.getDefinition().height());
        playerEntity.setFlag(EntityFlag.SNEAKING, sneaking);

        collisionManager.updatePlayerBoundingBox();
    }

    public void setSwimming(boolean swimming) {
        this.pose = swimming ? Pose.SWIMMING : Pose.STANDING;
        playerEntity.getDirtyMetadata().put(EntityData.BOUNDING_BOX_HEIGHT, swimming ? 0.6f : playerEntity.getDefinition().height());
        playerEntity.setFlag(EntityFlag.SWIMMING, swimming);
        playerEntity.updateBedrockMetadata();
    }

    public void setFlying(boolean flying) {
        this.flying = flying;

        if (sneaking) {
            // update bounding box as it is not reduced when flying
            setSneakingPose(!flying);
            playerEntity.updateBedrockMetadata();
        }
    }

    /**
     * Adjusts speed if the player is crawling.
     *
     * @return not null if attributes should be updated.
     */
    public AttributeData adjustSpeed() {
        AttributeData currentPlayerSpeed = playerEntity.getAttributes().get(GeyserAttributeType.MOVEMENT_SPEED);
        if (currentPlayerSpeed != null) {
            if ((pose.equals(Pose.SNEAKING) && !sneaking && collisionManager.isUnderSlab()) ||
                    (!swimmingInWater && playerEntity.getFlag(EntityFlag.SWIMMING) && !collisionManager.isPlayerInWater())) {
                // Either of those conditions means that Bedrock goes zoom when they shouldn't be
                AttributeData speedAttribute = GeyserAttributeType.MOVEMENT_SPEED.getAttribute(originalSpeedAttribute / 3.32f);
                playerEntity.getAttributes().put(GeyserAttributeType.MOVEMENT_SPEED, speedAttribute);
                return speedAttribute;
            } else if (originalSpeedAttribute != currentPlayerSpeed.getValue()) {
                // Speed has reset to normal
                AttributeData speedAttribute = GeyserAttributeType.MOVEMENT_SPEED.getAttribute(originalSpeedAttribute);
                playerEntity.getAttributes().put(GeyserAttributeType.MOVEMENT_SPEED, speedAttribute);
                return speedAttribute;
            }
        }
        return null;
    }

    /**
     * Will be overwritten for GeyserConnect.
     */
    protected void disableSrvResolving() {
        this.downstream.setFlag(BuiltinFlags.ATTEMPT_SRV_RESOLVE, false);
    }

    @Override
    public String name() {
        return authData.name();
    }

    @Override
    public UUID uuid() {
        return authData.uuid();
    }

    @Override
    public String xuid() {
        return authData.xuid();
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

    @Override
    public String getLocale() {
        return clientData.getLanguageCode();
     }

    public void setRenderDistance(int renderDistance) {
        renderDistance = GenericMath.ceil(++renderDistance * MathUtils.SQRT_OF_TWO); //square to circle
        this.renderDistance = renderDistance;

        ChunkRadiusUpdatedPacket chunkRadiusUpdatedPacket = new ChunkRadiusUpdatedPacket();
        chunkRadiusUpdatedPacket.setRadius(renderDistance);
        upstream.sendPacket(chunkRadiusUpdatedPacket);
    }

    public InetSocketAddress getSocketAddress() {
        return this.upstream.getAddress();
    }

    public void sendForm(Form form) {
        formCache.showForm(form);
    }

    public void sendForm(FormBuilder<?, ?> formBuilder) {
        formCache.showForm(formBuilder.build());
    }

    private void startGame() {
        StartGamePacket startGamePacket = new StartGamePacket();
        startGamePacket.setUniqueEntityId(playerEntity.getGeyserId());
        startGamePacket.setRuntimeEntityId(playerEntity.getGeyserId());
        startGamePacket.setPlayerGameType(switch (gameMode) {
            case CREATIVE -> GameType.CREATIVE;
            case ADVENTURE -> GameType.ADVENTURE;
            default -> GameType.SURVIVAL;
        });
        startGamePacket.setPlayerPosition(Vector3f.from(0, 69, 0));
        startGamePacket.setRotation(Vector2f.from(1, 1));

        startGamePacket.setSeed(-1);
        startGamePacket.setDimensionId(DimensionUtils.javaToBedrock(dimension));
        startGamePacket.setGeneratorId(1);
        startGamePacket.setLevelGameType(GameType.SURVIVAL);
        startGamePacket.setDifficulty(1);
        startGamePacket.setDefaultSpawn(Vector3i.ZERO);
        startGamePacket.setAchievementsDisabled(!geyser.getConfig().isXboxAchievementsEnabled());
        startGamePacket.setCurrentTick(-1);
        startGamePacket.setEduEditionOffers(0);
        startGamePacket.setEduFeaturesEnabled(false);
        startGamePacket.setRainLevel(0);
        startGamePacket.setLightningLevel(0);
        startGamePacket.setMultiplayerGame(true);
        startGamePacket.setBroadcastingToLan(true);
        startGamePacket.setPlatformBroadcastMode(GamePublishSetting.PUBLIC);
        startGamePacket.setXblBroadcastMode(GamePublishSetting.PUBLIC);
        startGamePacket.setCommandsEnabled(!geyser.getConfig().isXboxAchievementsEnabled());
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

        String serverName = geyser.getConfig().getBedrock().getServerName();
        startGamePacket.setLevelId(serverName);
        startGamePacket.setLevelName(serverName);

        startGamePacket.setPremiumWorldTemplateId("00000000-0000-0000-0000-000000000000");
        // startGamePacket.setCurrentTick(0);
        startGamePacket.setEnchantmentSeed(0);
        startGamePacket.setMultiplayerCorrelationId("");
        startGamePacket.setItemEntries(this.itemMappings.getItemEntries());
        startGamePacket.setVanillaVersion("*");
        startGamePacket.setInventoriesServerAuthoritative(true);
        startGamePacket.setServerEngine(""); // Do we want to fill this in?

        SyncedPlayerMovementSettings settings = new SyncedPlayerMovementSettings();
        settings.setMovementMode(AuthoritativeMovementMode.CLIENT);
        settings.setRewindHistorySize(0);
        settings.setServerAuthoritativeBlockBreaking(false);
        startGamePacket.setPlayerMovementSettings(settings);

        if (upstream.getProtocolVersion() <= Bedrock_v471.V471_CODEC.getProtocolVersion()) {
            startGamePacket.getExperiments().add(new ExperimentData("caves_and_cliffs", true));
        }

        upstream.sendPacket(startGamePacket);
    }

    /**
     * @return the next Bedrock item network ID to use for a new item
     */
    public int getNextItemNetId() {
        return itemNetId.getAndIncrement();
    }

    public void addTeleport(TeleportCache teleportCache) {
        teleportMap.put(teleportCache.getTeleportConfirmId(), teleportCache);

        ObjectIterator<Int2ObjectMap.Entry<TeleportCache>> it = teleportMap.int2ObjectEntrySet().iterator();

        // Remove any teleports with a higher number - maybe this is a world change that reset the ID to 0?
        while (it.hasNext()) {
            Int2ObjectMap.Entry<TeleportCache> entry = it.next();
            int nextID = entry.getValue().getTeleportConfirmId();
            if (nextID > teleportCache.getTeleportConfirmId()) {
                it.remove();
            }
        }
    }

    public void confirmTeleport(Vector3d position) {
        if (teleportMap.size() == 0) {
            return;
        }
        int teleportID = -1;

        for (Int2ObjectMap.Entry<TeleportCache> entry : teleportMap.int2ObjectEntrySet()) {
            if (entry.getValue().canConfirm(position)) {
                if (entry.getValue().getTeleportConfirmId() > teleportID) {
                    teleportID = entry.getValue().getTeleportConfirmId();
                }
            }
        }

        if (teleportID != -1) {
            ObjectIterator<Int2ObjectMap.Entry<TeleportCache>> it = teleportMap.int2ObjectEntrySet().iterator();

            // Confirm the current teleport and any earlier ones
            while (it.hasNext()) {
                TeleportCache entry = it.next().getValue();
                int nextID = entry.getTeleportConfirmId();
                if (nextID <= teleportID) {
                    ServerboundAcceptTeleportationPacket teleportConfirmPacket = new ServerboundAcceptTeleportationPacket(nextID);
                    sendDownstreamPacket(teleportConfirmPacket);
                    // Servers (especially ones like Hypixel) expect exact coordinates given back to them.
                    ServerboundMovePlayerPosRotPacket positionPacket = new ServerboundMovePlayerPosRotPacket(playerEntity.isOnGround(),
                            entry.getX(), entry.getY(), entry.getZ(), entry.getYaw(), entry.getPitch());
                    sendDownstreamPacket(positionPacket);
                    it.remove();
                    geyser.getLogger().debug("Confirmed teleport " + nextID);
                }
            }
        }

        if (teleportMap.size() > 0) {
            int resendID = -1;
            for (Int2ObjectMap.Entry<TeleportCache> entry : teleportMap.int2ObjectEntrySet()) {
                TeleportCache teleport = entry.getValue();
                teleport.incrementUnconfirmedFor();
                if (teleport.shouldResend()) {
                    if (teleport.getTeleportConfirmId() >= resendID) {
                        resendID = teleport.getTeleportConfirmId();
                    }
                }
            }

            if (resendID != -1) {
                geyser.getLogger().debug("Resending teleport " + resendID);
                TeleportCache teleport = teleportMap.get(resendID);
                getPlayerEntity().moveAbsolute(Vector3f.from(teleport.getX(), teleport.getY(), teleport.getZ()),
                        teleport.getYaw(), teleport.getPitch(), playerEntity.isOnGround(), true);
            }
        }
    }

    /**
     * Queue a packet to be sent to player.
     *
     * @param packet the bedrock packet from the NukkitX protocol lib
     */
    public void sendUpstreamPacket(BedrockPacket packet) {
        upstream.sendPacket(packet);
    }

    /**
     * Send a packet immediately to the player.
     *
     * @param packet the bedrock packet from the NukkitX protocol lib
     */
    public void sendUpstreamPacketImmediately(BedrockPacket packet) {
        upstream.sendPacketImmediately(packet);
    }

    /**
     * Send a packet to the remote server.
     *
     * @param packet the java edition packet from MCProtocolLib
     */
    public void sendDownstreamPacket(Packet packet) {
        if (!closed && this.downstream != null) {
            Channel channel = this.downstream.getChannel();
            if (channel == null) {
                // Channel is only null before the connection has initialized
                geyser.getLogger().warning("Tried to send a packet to the Java server too early!");
                if (geyser.getConfig().isDebugMode()) {
                    Thread.dumpStack();
                }
                return;
            }

            EventLoop eventLoop = channel.eventLoop();
            if (eventLoop.inEventLoop()) {
                sendDownstreamPacket0(packet);
            } else {
                eventLoop.execute(() -> sendDownstreamPacket0(packet));
            }
        }
    }

    private void sendDownstreamPacket0(Packet packet) {
        if (protocol.getState().equals(ProtocolState.GAME) || packet.getClass() == ServerboundCustomQueryPacket.class) {
            downstream.send(packet);
        } else {
            geyser.getLogger().debug("Tried to send downstream packet " + packet.getClass().getSimpleName() + " before connected to the server");
        }
    }

    /**
     * Update the cached value for the reduced debug info gamerule.
     * If enabled, also hides the player's coordinates.
     *
     * @param value The new value for reducedDebugInfo
     */
    public void setReducedDebugInfo(boolean value) {
        reducedDebugInfo = value;
        // Set the showCoordinates data. This is done because updateShowCoordinates() uses this gamerule as a variable.
        preferencesCache.updateShowCoordinates();
    }

    /**
     * Changes the daylight cycle gamerule on the client
     * This is used in the login screen along-side normal usage
     *
     * @param doCycle If the cycle should continue
     */
    public void setDaylightCycle(boolean doCycle) {
        sendGameRule("dodaylightcycle", doCycle);
        // Save the value so we don't have to constantly send a daylight cycle gamerule update
        this.daylightCycle = doCycle;
    }

    /**
     * Send a gamerule value to the client
     *
     * @param gameRule The gamerule to send
     * @param value    The value of the gamerule
     */
    public void sendGameRule(String gameRule, Object value) {
        GameRulesChangedPacket gameRulesChangedPacket = new GameRulesChangedPacket();
        gameRulesChangedPacket.getGameRules().add(new GameRuleData<>(gameRule, value));
        upstream.sendPacket(gameRulesChangedPacket);
    }

    /**
     * Checks if the given session's player has a permission
     *
     * @param permission The permission node to check
     * @return true if the player has the requested permission, false if not
     */
    @Override
    public boolean hasPermission(String permission) {
        return geyser.getWorldManager().hasPermission(this, permission);
    }

    /**
     * Send an AdventureSettingsPacket to the client with the latest flags
     */
    public void sendAdventureSettings() {
        AdventureSettingsPacket adventureSettingsPacket = new AdventureSettingsPacket();
        adventureSettingsPacket.setUniqueEntityId(playerEntity.getGeyserId());
        // Set command permission if OP permission level is high enough
        // This allows mobile players access to a GUI for doing commands. The commands there do not change above OPERATOR
        // and all commands there are accessible with OP permission level 2
        adventureSettingsPacket.setCommandPermission(opPermissionLevel >= 2 ? CommandPermission.OPERATOR : CommandPermission.NORMAL);
        // Required to make command blocks destroyable
        adventureSettingsPacket.setPlayerPermission(opPermissionLevel >= 2 ? PlayerPermission.OPERATOR : PlayerPermission.MEMBER);

        // Update the noClip and worldImmutable values based on the current gamemode
        boolean spectator = gameMode == GameMode.SPECTATOR;
        boolean worldImmutable = gameMode == GameMode.ADVENTURE || spectator;

        Set<AdventureSetting> flags = adventureSettingsPacket.getSettings();
        if (canFly || spectator) {
            flags.add(AdventureSetting.MAY_FLY);
        }

        if (flying || spectator) {
            if (spectator && !flying) {
                // We're "flying locked" in this gamemode
                flying = true;
                ServerboundPlayerAbilitiesPacket abilitiesPacket = new ServerboundPlayerAbilitiesPacket(true);
                sendDownstreamPacket(abilitiesPacket);
            }
            flags.add(AdventureSetting.FLYING);
        }

        if (worldImmutable) {
            flags.add(AdventureSetting.WORLD_IMMUTABLE);
        }

        if (spectator) {
            flags.add(AdventureSetting.NO_CLIP);
        }

        flags.add(AdventureSetting.AUTO_JUMP);

        sendUpstreamPacket(adventureSettingsPacket);
    }

    /**
     * Used for updating statistic values since we only get changes from the server
     *
     * @param statistics Updated statistics values
     */
    public void updateStatistics(@NonNull Map<Statistic, Integer> statistics) {
        this.statistics.putAll(statistics);
    }

    public void refreshEmotes(List<UUID> emotes) {
        this.emotes.addAll(emotes);
        for (GeyserSession player : geyser.getSessionManager().getSessions().values()) {
            List<UUID> pieces = new ArrayList<>();
            for (UUID piece : emotes) {
                if (!player.getEmotes().contains(piece)) {
                    pieces.add(piece);
                }
                player.getEmotes().add(piece);
            }
            EmoteListPacket emoteList = new EmoteListPacket();
            emoteList.setRuntimeEntityId(player.getPlayerEntity().getGeyserId());
            emoteList.getPieceIds().addAll(pieces);
            player.sendUpstreamPacket(emoteList);
        }
    }
}
