/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

import com.google.gson.JsonObject;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.exception.MinecraftProfileNotFoundException;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.util.MinecraftAuth4To5Migrator;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector2i;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.netty.channel.raknet.RakChildChannel;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.cloudburstmc.protocol.bedrock.BedrockDisconnectReasons;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer;
import org.cloudburstmc.protocol.bedrock.data.ChatRestrictionLevel;
import org.cloudburstmc.protocol.bedrock.data.ExperimentData;
import org.cloudburstmc.protocol.bedrock.data.GamePublishSetting;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.cloudburstmc.protocol.bedrock.data.ServerConfigurationJoinInfo;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.SpawnBiomeType;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.protocol.bedrock.data.command.SoftEnumUpdateType;
import org.cloudburstmc.protocol.bedrock.data.definitions.DimensionDefinition;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.CraftingRecipeData;
import org.cloudburstmc.protocol.bedrock.packet.AvailableEntityIdentifiersPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BiomeDefinitionListPacket;
import org.cloudburstmc.protocol.bedrock.packet.CameraPresetsPacket;
import org.cloudburstmc.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.cloudburstmc.protocol.bedrock.packet.DimensionDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.GameRulesChangedPacket;
import org.cloudburstmc.protocol.bedrock.packet.ItemComponentPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetCommandsEnabledPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetTimePacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.bedrock.packet.SyncEntityPropertyPacket;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;
import org.cloudburstmc.protocol.bedrock.packet.TransferPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAdventureSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateClientInputLocksPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateSoftEnumPacket;
import org.cloudburstmc.protocol.bedrock.packet.VoxelShapesPacket;
import org.cloudburstmc.protocol.common.util.OptionalBoolean;
import org.geysermc.api.util.BedrockPlatform;
import org.geysermc.api.util.InputMode;
import org.geysermc.api.util.UiProfile;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.bedrock.camera.CameraData;
import org.geysermc.geyser.api.bedrock.camera.CameraShake;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.EntityData;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.api.entity.type.player.GeyserPlayerEntity;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.network.RemoteServer;
import org.geysermc.geyser.api.skin.SkinData;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.GeyserEntityData;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.erosion.AbstractGeyserboundPacketHandler;
import org.geysermc.geyser.erosion.ErosionCancellationException;
import org.geysermc.geyser.erosion.GeyserboundHandshakePacketHandler;
import org.geysermc.geyser.event.type.SessionDisconnectEventImpl;
import org.geysermc.geyser.impl.camera.CameraDefinitions;
import org.geysermc.geyser.impl.camera.GeyserCameraData;
import org.geysermc.geyser.input.InputLocksFlag;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.InventoryHolder;
import org.geysermc.geyser.inventory.LecternContainer;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.inventory.recipe.GeyserRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserSmithingRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserStonecutterData;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.BlockItem;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.level.gamerule.GameRuleHandler;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.network.netty.LocalSession;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.auth.AuthData;
import org.geysermc.geyser.session.auth.BedrockClientData;
import org.geysermc.geyser.session.cache.AdvancementsCache;
import org.geysermc.geyser.session.cache.BlockBreakHandler;
import org.geysermc.geyser.session.cache.BookEditCache;
import org.geysermc.geyser.session.cache.BundleCache;
import org.geysermc.geyser.session.cache.ChunkCache;
import org.geysermc.geyser.session.cache.ComponentCache;
import org.geysermc.geyser.session.cache.EntityCache;
import org.geysermc.geyser.session.cache.EntityEffectCache;
import org.geysermc.geyser.session.cache.FormCache;
import org.geysermc.geyser.session.cache.InputCache;
import org.geysermc.geyser.session.cache.LodestoneCache;
import org.geysermc.geyser.session.cache.PistonCache;
import org.geysermc.geyser.session.cache.PreferencesCache;
import org.geysermc.geyser.session.cache.RegistryCache;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.geyser.session.cache.StructureBlockCache;
import org.geysermc.geyser.session.cache.TagCache;
import org.geysermc.geyser.session.cache.TeleportCache;
import org.geysermc.geyser.session.cache.WorldBorder;
import org.geysermc.geyser.session.cache.WorldCache;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.tags.DialogTag;
import org.geysermc.geyser.session.cache.waypoint.GeyserWaypoint;
import org.geysermc.geyser.session.cache.waypoint.WaypointCache;
import org.geysermc.geyser.session.dialog.BuiltInDialog;
import org.geysermc.geyser.session.dialog.Dialog;
import org.geysermc.geyser.session.dialog.DialogManager;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.geyser.util.CooldownUtils;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.geyser.util.LoginEncryptionUtils;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.BuiltinFlags;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.session.ClientNetworkSession;
import org.geysermc.mcprotocollib.protocol.ClientListener;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.ServerLink;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.HandPreference;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ChatVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ParticleStatus;
import org.geysermc.mcprotocollib.protocol.data.game.setting.SkinPart;
import org.geysermc.mcprotocollib.protocol.data.game.statistic.CustomStatistic;
import org.geysermc.mcprotocollib.protocol.data.game.statistic.Statistic;
import org.geysermc.mcprotocollib.protocol.data.handshake.HandshakeIntent;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundAcceptCodeOfConductPacket;
import org.geysermc.mcprotocollib.protocol.packet.cookie.serverbound.ServerboundCookieResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundCustomQueryAnswerPacket;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class GeyserSession implements GeyserConnection, GeyserCommandSource {

    private final GeyserImpl geyser;
    private final UpstreamSession upstream;
    private DownstreamSession downstream;
    /**
     * The loop where all packets and ticking is processed to prevent concurrency issues.
     * If this is manually called, ensure that any exceptions are properly handled.
     */
    private final EventLoop tickEventLoop;
    @Setter
    private AuthData authData;
    private BedrockClientData clientData;
    /**
     * Used for Floodgate skin uploading
     */
    @Setter
    private List<String> certChainData;
    @Setter
    private String token;

    @NonNull
    @Setter
    private volatile AbstractGeyserboundPacketHandler erosionHandler;

    @Accessors(fluent = true)
    @Setter
    private RemoteServer remoteServer;

    private final SessionPlayerEntity playerEntity;

    private final AdvancementsCache advancementsCache;
    private final BookEditCache bookEditCache;
    private final BundleCache bundleCache;
    private final ChunkCache chunkCache;
    private final ComponentCache componentCache;
    private final EntityCache entityCache;
    private final EntityEffectCache effectCache;
    private final FormCache formCache;
    private final GameRuleHandler gameRuleHandler;
    private final InputCache inputCache;
    private final LodestoneCache lodestoneCache;
    private final PistonCache pistonCache;
    private final PreferencesCache preferencesCache;
    private final RegistryCache registryCache;
    private final SkullCache skullCache;
    private final StructureBlockCache structureBlockCache;
    private final TagCache tagCache;
    private final WaypointCache waypointCache;
    private final WorldCache worldCache;

    /**
     * Handles block breaking and break animation progress caching.
     */
    @Setter
    private BlockBreakHandler blockBreakHandler;

    @Setter
    private TeleportCache unconfirmedTeleport;

    private final WorldBorder worldBorder;
    /**
     * Whether simulated fog has been sent to the client or not.
     */
    private boolean isInWorldBorderWarningArea = false;

    /**
     * Stores the player inventory and player inventory translator
     */
    private final InventoryHolder<PlayerInventory> playerInventoryHolder;

    /**
     * Stores the current open Bedrock inventory, including the correct translator.
     * Prefer using {@link InventoryUtils#getInventory(GeyserSession, int)}, as this
     * method can e.g. return a {@code InventoryHolder<LecternContainer>} due to the
     * workaround in {@link LecternContainer#isBookInPlayerInventory()} workaround.
     */
    @Setter
    private @Nullable InventoryHolder<? extends Inventory> inventoryHolder;

    private final DialogManager dialogManager = new DialogManager(this);

    /**
     * A list of links sent to us by the server in the server links packet.
     */
    @Setter
    private List<ServerLink> serverLinks = List.of();

    /**
     * A list of commands known to the client. These are all the commands that have been sent to us by the server.
     */
    @Setter
    private List<String> knownCommands = List.of();
    /**
     * A list of "restricted" commands known to the client. These are all the commands that have been sent to us by the server, and require some sort of elevated permissions.
     */
    @Setter
    private List<String> restrictedCommands = List.of();

    /**
     * Whether the client is currently closing an inventory.
     * Used to open new inventories while another one is currently open.
     */
    @Setter
    private boolean closingInventory;

    /**
     * Stores the bedrock inventory id of the pending inventory, or -1 if no inventory is pending.
     * This id is only set when the block that should be opened exists.
     */
    @Setter
    private int pendingOrCurrentBedrockInventoryId = -1;

    /**
     * A check for whether or not we need to clear the attack cooldown we emulate.
     */
    @Setter
    private boolean needAttackCooldownClear = false;

    /**
     * Use {@link #getNextItemNetId()} instead for consistency
     */
    @Getter(AccessLevel.NONE)
    private final AtomicInteger itemNetId = new AtomicInteger(2);

    @Setter
    private ScheduledFuture<?> containerOutputFuture;

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

    /**
     * A map of Vector3i positions to Java entities.
     * Used for translating Bedrock block actions to Java entity actions.
     */
    private final Map<Vector3i, ItemFrameEntity> itemFrameCache = new Object2ObjectOpenHashMap<>();

    /**
     * A map of all players (and their heads) that are wearing a player head with a custom texture.
     * Our workaround for these players is to give them a custom skin and geometry to emulate wearing a custom skull.
     */
    private final Map<UUID, ResolvableProfile> playerWithCustomHeads = new Object2ObjectOpenHashMap<>();

    @Setter
    private boolean droppingLecternBook;

    @Setter
    private Vector2i lastChunkPosition = null;
    private int clientRenderDistance = -1;
    private int serverRenderDistance = -1;

    // Exposed for GeyserConnect usage
    protected boolean sentSpawnPacket;

    // Exposed for 3p server usage
    @Setter
    private @Nullable ServerConfigurationJoinInfo serverJoinInfo = null;

    boolean loggedIn;
    boolean loggingIn;

    @Setter
    private boolean spawned;
    /**
     * Accessed on the initial Java and Bedrock packet processing threads
     */
    private volatile boolean closed;

    private GameMode gameMode = GameMode.SURVIVAL;

    /**
     * Keeps track of the world name for respawning.
     */
    @Setter
    private Key worldName = null;
    /**
     * As of Java 1.19.3, the client only uses these for commands.
     */
    @Setter
    private String[] levels;

    private boolean sneaking;

    /**
     * Used to send a shift state for a tick to dismount from entitites
     */
    @Setter
    private boolean shouldSendSneak;

    /**
     * Stores the Java pose that the server and/or Geyser believes the player currently has.
     */
    @Setter
    private Pose pose = Pose.STANDING;

    /**
     * This is used to keep track of player sprinting and should only change by START_SPRINT and STOP_SPRINT sent by the player, not from flag update.
     */
    @Setter
    private boolean sprinting;

    /**
     * The overworld dimension which Bedrock Edition uses.
     */
    private BedrockDimension bedrockOverworldDimension = BedrockDimension.OVERWORLD;
    /**
     * The dimension of the player.
     * As all entities are in the same world, this can be safely applied to all other entities.
     */
    @MonotonicNonNull
    @Setter
    private JavaDimension dimensionType = null;
    /**
     * Which dimension Bedrock understands themselves to be in.
     * This should only be set after the ChangeDimensionPacket is sent, or
     * right before the StartGamePacket is sent.
     */
    @Setter
    private BedrockDimension bedrockDimension = this.bedrockOverworldDimension;

    @Setter
    private Vector3i lastBlockPlacePosition;

    @Setter
    private BlockItem lastBlockPlaced;

    @Setter
    private boolean interacting;

    /**
     * Stores the last position of the block the player interacted with. This can either be a block that the client
     * placed or an existing block the player interacted with (for example, a chest). <br>
     * Initialized as (0, 0, 0) so it is always not-null.
     */
    @Setter
    private Vector3i lastInteractionBlockPosition = Vector3i.ZERO;

    @Setter
    private int lastInteractionBlockFace = -1;

    /**
     * Stores the Java position of the player the last time they interacted.
     * Used to verify that the player did not move since their last interaction. <br>
     * Initialized as (0, 0, 0) so it is always not-null.
     */
    @Setter
    private Vector3f lastInteractionPlayerPosition = Vector3f.ZERO;

    /**
     * The entity that the client is currently looking at.
     */
    @Setter
    private Entity mouseoverEntity;

    /**
     * Stores all Java recipes by ID, and matches them to all possible Bedrock recipe identifiers.
     */
    private final Int2ObjectMap<List<String>> javaToBedrockRecipeIds = new Int2ObjectOpenHashMap<>();

    private final Int2ObjectMap<GeyserRecipe> craftingRecipes = new Int2ObjectOpenHashMap<>();
    @Setter
    private Pair<CraftingRecipeData, GeyserRecipe> lastCreatedRecipe = null; // TODO try to prevent sending duplicate recipes
    private final AtomicInteger lastRecipeNetId;

    /**
     * Used to minimize the amount of recipes sent to the client, as the packet is quite heavy
     * when sent in rapid succession
     */
    @Setter
    private boolean cleanRecipesRequired = true;

    /**
     * Saves a list of all stonecutter recipes, for use in a stonecutter inventory.
     * The key is the Bedrock recipe net ID; the values are their respective output and button ID.
     */
    @Setter
    private Int2ObjectMap<GeyserStonecutterData> stonecutterRecipes = Int2ObjectMaps.emptyMap();
    private final List<GeyserSmithingRecipe> smithingRecipes = new ArrayList<>();

    /**
     * Whether to work around 1.13's different behavior in villager trading menus.
     */
    @Setter
    private boolean emulatePost1_13Logic = true;
    /**
     * Starting in 1.17, Java servers expect the <code>carriedItem</code> parameter of the serverbound click container
     * packet to be the current contents of the mouse after the transaction has been done. 1.16 expects the clicked slot
     * contents before any transaction is done. With the current ViaVersion structure, if we do not send what 1.16 expects
     * and send multiple click container packets, then successive transactions will be rejected.
     */
    @Setter
    private boolean emulatePost1_16Logic = true;
    @Setter
    private boolean emulatePost1_18Logic = true;

    /**
     * Whether to emulate pre-1.20 smithing table behavior.
     * Adapts ViaVersion's furnace UI to one Bedrock can use.
     * See {@link org.geysermc.geyser.translator.inventory.OldSmithingTableTranslator}.
     */
    @Setter
    private boolean oldSmithingTable = false;

    /**
     * Whether to use the minecart_improvements experiment
     */
    @Setter
    private boolean isUsingExperimentalMinecartLogic = false;

    /**
     * Whether a fishing bobber in the world is connected to the player. Used for custom items, updates the item the player is holding when changed.
     */
    private boolean hasFishingRodCast = false;

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
     * Stores whether the player is inside a client predicted vehicle.
     */
    @Getter
    @Setter
    private boolean inClientPredictedVehicle;

    /**
     * Store the last time the player interacted. Used to fix a right-click spam bug.
     * See <a href="https://github.com/GeyserMC/Geyser/issues/503">this</a> for context.
     */
    @Setter
    private long lastInteractionTime;

    /**
     * Stores whether the player intended to place a bucket.
     */
    @Setter
    private boolean placedBucket;

    /**
     * Stores whether we've updated a lower door block
     */
    @Setter
    private @Nullable Vector3i lastLowerDoorPosition = null;

    /**
     * Counts how many ticks have occurred since an arm animation started.
     * -1 means there is no active arm swing
     */
    private int armAnimationTicks = -1;

    /**
     * The tick in which the player last hit air.
     * Used to ensure we dont send two sing packets for one hit.
     */
    @Setter
    private int lastAirHitTick;

    /**
     * Keep track of fireworks rockets that are attached to the player.
     * Used to keep track of attached fireworks rocket and improve fireworks rocket boosting parity.
     */
    private final List<Long> attachedFireworkRockets = new CopyOnWriteArrayList<>();

    /**
     * Controls whether the daylight cycle gamerule has been sent to the client, so the sun/moon remain motionless.
     */
    private boolean shouldClientTickClock = true;

    private boolean reducedDebugInfo = false;

    /**
     * The op permission level set by the server
     */
    private int opPermissionLevel = 0;

    /**
     * If the current player can fly
     */
    @Setter
    private boolean canFly = false;

    /**
     * If the current player is flying
     */
    @Setter
    private boolean flying = false;

    /**
     * If the current player should be able to noclip through blocks, this is used for void floor workaround and not spectator.
     */
    private boolean noClip = false;

    @Setter
    private boolean instabuild = false;

    @Setter
    private float flySpeed;
    @Setter
    private float walkSpeed;

    /**
     * Caches current rain strength.
     * Value between 0 and 1.
     */
    private float rainStrength = 0.0f;

    /**
     * Caches current thunder strength.
     * Value between 0 and 1.
     */
    private float thunderStrength = 0.0f;

    /**
     * Stores a map of all statistics sent from the server.
     * The server only sends new statistics back to us, so in order to show all statistics we need to cache existing ones.
     */
    private final Object2IntMap<Statistic> statistics = new Object2IntOpenHashMap<>(0);

    /**
     * Whether we're expecting statistics to be sent back to us.
     */
    @Setter
    private boolean waitingForStatistics = false;

    /**
     * Whether advanced tooltips will be added to the player's items.
     */
    @Setter
    private boolean advancedTooltips = false;

    /**
     * The thread that will run every game tick.
     */
    private ScheduledFuture<?> tickThread = null;

    /**
     * The number of ticks that have elapsed since the start of this session
     */
    private int ticks;

    /**
     * The number of ticks that have elapsed since the start of this session according to the client.
     */
    @Setter
    private long clientTicks;

    /**
     * The game ticks counter according to the server.
     *
     * <p>This is probably the counter you want to use when dealing with ticks on the server. The day time counters can
     * change and can have a custom rate, however this counter never changes in a dimension, and always has the same rate (1 tick every 20th of a second),
     * pending any server lag.</p>
     *
     * <p>Note: The TickingStatePacket is currently ignored.</p>
     */
    @Setter
    private long gameTicks;

    /**
     * The day time in ticks according to the server.
     *
     * <p>Note: The TickingStatePacket is currently ignored.</p>
     */
    private long dayTimeTicks;

    /**
     * The partial day time tick according to the server.
     *
     * <p>Note: The TickingStatePacket is currently ignored.</p>
     */
    private double partialTimeTick;

    /**
     * How fast the world time should pass according to the server. Starts at 0, server will tell us what it is when joining.
     */
    private float clockRate = 0.0F;

    /**
     * Used to return players back to their vehicles if the server doesn't want them unmounting.
     */
    @Setter
    private ScheduledFuture<?> mountVehicleScheduledFuture = null;

    /**
     * A cache of IDs from ClientboundKeepAlivePackets or ClientboundPingPacket that have been sent to the Bedrock client, but haven't been returned to the server.
     * Only used if {@link GeyserConfig.GameplayConfig#forwardPlayerPing()} is enabled.
     */
    private final Queue<Runnable> latencyPingCache = new ConcurrentLinkedQueue<>();

    /**
     * Queued packets to be sent immediately at the end of each tick.
     */
    private final List<BedrockPacket> queuedImmediatelyPackets = new ArrayList<>();

    /**
     * Stores the book that is currently being read. Used in {@link org.geysermc.geyser.translator.protocol.java.inventory.JavaOpenBookTranslator}
     */
    @Setter
    private @Nullable ItemData currentBook = null;

    /**
     * Stores cookies sent by the Java server.
     */
    @Setter
    private Map<String, byte[]> cookies = new Object2ObjectOpenHashMap<>();

    private final GeyserCameraData cameraData;

    private final GeyserEntityData entityData;

    @Getter(AccessLevel.PACKAGE)
    private MinecraftProtocol protocol;

    private int nanosecondsPerTick = 50000000;
    private float millisecondsPerTick = 50.0f;
    private boolean tickingFrozen = false;
    /**
     * The amount of ticks requested by the server that the game should proceed with, even if the game tick loop is frozen.
     */
    @Setter
    private int stepTicks = 0;

    @Setter
    private boolean allowVibrantVisuals = true;

    @Accessors(fluent = true)
    @Setter
    private boolean hasAcceptedCodeOfConduct = false;

    @Accessors(fluent = true)
    @Setter
    private boolean integratedPackActive = false;

    private final Set<InputLocksFlag> inputLocksSet = EnumSet.noneOf(InputLocksFlag.class);
    private boolean inputLockDirty;

    public GeyserSession(GeyserImpl geyser, BedrockServerSession bedrockServerSession, EventLoop tickEventLoop) {
        this.geyser = geyser;
        this.upstream = new UpstreamSession(bedrockServerSession);
        this.tickEventLoop = tickEventLoop;

        this.erosionHandler = new GeyserboundHandshakePacketHandler(this);

        this.advancementsCache = new AdvancementsCache(this);
        this.bookEditCache = new BookEditCache(this);
        this.bundleCache = new BundleCache(this);
        this.chunkCache = new ChunkCache(this);
        this.componentCache = new ComponentCache(this);
        this.entityCache = new EntityCache(this);
        this.effectCache = new EntityEffectCache();
        this.formCache = new FormCache(this);
        this.inputCache = new InputCache(this);
        this.lodestoneCache = new LodestoneCache();
        this.pistonCache = new PistonCache(this);
        this.preferencesCache = new PreferencesCache(this);
        this.registryCache = new RegistryCache(this);
        this.skullCache = new SkullCache(this);
        this.structureBlockCache = new StructureBlockCache();
        this.tagCache = new TagCache(this);
        this.waypointCache = new WaypointCache(this);
        this.worldCache = new WorldCache(this);
        this.cameraData = new GeyserCameraData(this);
        this.entityData = new GeyserEntityData(this);

        this.collisionManager = new CollisionManager(this);
        this.gameRuleHandler = new GameRuleHandler(this);
        this.blockBreakHandler = new BlockBreakHandler(this);
        this.worldBorder = new WorldBorder(this);

        this.playerEntity = new SessionPlayerEntity(this);
        collisionManager.updatePlayerBoundingBox(this.playerEntity.position());

        this.playerInventoryHolder = new InventoryHolder<>(this, new PlayerInventory(this), InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR);
        this.inventoryHolder = null;
        this.lastRecipeNetId = new AtomicInteger(InventoryUtils.LAST_RECIPE_NET_ID + 1);

        this.spawned = false;
        this.loggedIn = false;

        this.remoteServer = geyser.defaultRemoteServer();
    }

    // ... (all other methods remain the same until doSendForm) ...

    @Override
    public boolean sendForm(@NonNull Form form) {
        // First close any dialogs that are open. This won't execute the dialog's closing action.
        dialogManager.close();
        return doSendForm(form);
    }

    /**
     * Sends a form without first closing any open dialog. This should only be used by {@link org.geysermc.geyser.session.dialog.Dialog}s.
     */
    public void sendDialogForm(@NonNull Form form) {
        doSendForm(form);
    }

    private boolean doSendForm(@NonNull Form form) {
        // Close all currently open forms.
        if (formCache.hasFormOpen()) {
            closeForm();
        }

        // Cache this form, let's see whether we can open it immediately
        formCache.addForm(form);

        // Also close current inventories, otherwise the form will not show
        // BUT: Do NOT close inventory when showing a Dialog (Java 1.21+ behavior)
        // This fixes the bug where inventories close on Bedrock/Geyser but not on Java.
        if (inventoryHolder != null && !isShowingDialog()) {
            // We'll open the form when the client confirms current inventory being closed
            InventoryUtils.sendJavaContainerClose(inventoryHolder);
            InventoryUtils.closeInventory(this, inventoryHolder, true);
        }

        // Open the current form, unless we're in the process of closing another
        // If we're waiting, the form will be sent when Bedrock confirms closing
        // If we don't wait, the client rejects the form as it is busy
        if (!isClosingInventory() && upstream.isInitialized()) {
            formCache.resendAllForms();
        }

        return true;
    }

    /**
     * Returns whether we are currently showing (or about to show) a Dialog.
     * Used to preserve inventory state for Java 1.21+ dialog parity (inventory stays open underneath dialog).
     */
    private boolean isShowingDialog() {
        return dialogManager.isDialogOpen();
    }

    // The rest of the file continues unchanged...
    public void acceptCodeOfConduct() {
        if (hasAcceptedCodeOfConduct) {
            return;
        }
        hasAcceptedCodeOfConduct = true;
        sendDownstreamConfigurationPacket(ServerboundAcceptCodeOfConductPacket.INSTANCE);
    }

    // ... (remaining methods) ...
}
