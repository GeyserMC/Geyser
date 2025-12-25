/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.configuration;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.api.network.BedrockListener;
import org.geysermc.geyser.api.network.RemoteServer;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.text.AsteriskSerializer;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.CooldownUtils;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultBoolean;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultNumeric;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultString;
import org.spongepowered.configurate.interfaces.meta.range.NumericRange;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ConfigSerializable
public interface GeyserConfig {
    @Comment("Network settings for the Bedrock listener")
    BedrockConfig bedrock();

    @Comment("Network settings for the Java server connection")
    JavaConfig java();

    @Comment("MOTD settings")
    MotdConfig motd();

    @Comment("Gameplay options that affect Bedrock players")
    GameplayConfig gameplay();

    @Comment("The default locale if we don't have the one the client requested. If set to \"system\", the system's language will be used.")
    @DefaultString(GeyserLocale.SYSTEM_LOCALE)
    @NonNull
    String defaultLocale();

    @Comment("Whether player IP addresses will be logged by the server.")
    @DefaultBoolean(true)
    boolean logPlayerIpAddresses();

    @Comment("""
            For online mode authentication type only.
            Stores a list of Bedrock player usernames that should have their Java Edition account saved after login.
            This saves a token that can be reused to authenticate the player later. This does not save emails or passwords,
            but you should still be cautious when adding to this list and giving others access to this Geyser instance's files.
            Removing a name from this list will delete its cached login information on the next Geyser startup.
            The file that tokens will be saved in is in the same folder as this config, named "saved-refresh-tokens.json".""")
    default List<String> savedUserLogins() {
        return List.of("ThisExampleUsernameShouldBeLongEnoughToNeverBeAnXboxUsername",
            "ThisOtherExampleUsernameShouldAlsoBeLongEnough");
    }

    @Comment("""
            For online mode authentication type only.
            Specify how many seconds to wait while user authorizes Geyser to access their Microsoft account.
            User is allowed to disconnect from the server during this period.""")
    @DefaultNumeric(120)
    int pendingAuthenticationTimeout();

    @Comment("""
            Whether to alert the console and operators that a new Geyser version is available that supports a Bedrock version
            that this Geyser version does not support. It's recommended to keep this option enabled, as many Bedrock platforms
            auto-update.""")
    @DefaultBoolean(true)
    boolean notifyOnNewBedrockUpdate();

    @Comment("Advanced configuration options. These usually do not need modifications.")
    AdvancedConfig advanced();

    @Comment("""
            bStats is a stat tracker that is entirely anonymous and tracks only basic information
            about Geyser, such as how many people are online, how many servers are using Geyser,
            what OS is being used, etc. You can learn more about bStats here: https://bstats.org/.
            https://bstats.org/plugin/server-implementation/GeyserMC""")
    @DefaultBoolean(true)
    @ExcludePlatform(platforms = {"BungeeCord", "Spigot", "Velocity"}) // bStats platform versions used
    boolean enableMetrics();

    @Comment("The bstats metrics uuid. Do not touch!")
    @ExcludePlatform(platforms = {"BungeeCord", "Spigot", "Velocity"}) // bStats platform versions used
    default UUID metricsUuid() {
        return UUID.randomUUID();
    }

    @Comment("If debug messages should be sent through console")
    boolean debugMode();

    @Comment("Do not change!")
    @SuppressWarnings("unused")
    default int configVersion() {
        return Constants.CONFIG_VERSION;
    }

    @ConfigSerializable
    interface BedrockConfig extends BedrockListener {
        @Comment("""
                The IP address that Geyser will bind on to listen for incoming Bedrock connections.
                Generally, you should only change this if you want to limit what IPs can connect to your server.""")
        @NonNull
        @Override
        @DefaultString("0.0.0.0")
        @AsteriskSerializer.Asterisk
        String address();

        @Comment("""
            The port that will Geyser will listen on for incoming Bedrock connections.
            Since Minecraft: Bedrock Edition uses UDP, this port must allow UDP traffic.""")
        @Override
        @DefaultNumeric(19132)
        @NumericRange(from = 0, to = 65535)
        int port();

        @Comment("""
                Some hosting services change your Java port everytime you start the server and require the same port to be used for Bedrock.
                This option makes the Bedrock port the same as the Java port every time you start the server.""")
        @DefaultBoolean
        @PluginSpecific
        boolean cloneRemotePort();

        void address(String address);
        void port(int port);

        @Exclude
        @Override
        default int broadcastPort() {
            return GeyserImpl.getInstance().config().advanced().bedrock().broadcastPort();
        }

        @Exclude
        @Override
        default String primaryMotd() {
            return GeyserImpl.getInstance().config().motd().primaryMotd();
        }

        @Exclude
        @Override
        default String secondaryMotd() {
            return GeyserImpl.getInstance().config().motd().secondaryMotd();
        }

        @Exclude
        @Override
        default String serverName() {
            return GeyserImpl.getInstance().config().gameplay().serverName();
        }
    }

    @ConfigSerializable
    interface JavaConfig extends RemoteServer {
        void address(String address);
        void port(int port);

        @Comment("""
                What type of authentication Bedrock players will be checked against when logging into the Java server.
                Can be "floodgate" (see https://wiki.geysermc.org/floodgate/), "online", or "offline".""")
        @NonNull
        @Override
        default AuthType authType() {
            return AuthType.ONLINE;
        }

        void authType(AuthType authType);
        boolean forwardHostname();

        @Override
        @Exclude
        default String minecraftVersion() {
            return GameProtocol.getJavaMinecraftVersion();
        }

        @Override
        @Exclude
        default int protocolVersion() {
            return GameProtocol.getJavaProtocolVersion();
        }

        @Override
        @Exclude
        default boolean resolveSrv() {
            return false;
        }
    }

    @ConfigSerializable
    interface MotdConfig {
        @Comment("""
            The MOTD that will be broadcasted to Minecraft: Bedrock Edition clients. This is irrelevant if "passthrough-motd" is set to true.
            If either of these are empty, the respective string will default to "Geyser\"""")
        @DefaultString("Geyser")
        String primaryMotd();
        @DefaultString("Another Geyser server.")
        String secondaryMotd();

        @Comment("Whether Geyser should relay the MOTD from the Java server to Bedrock players.")
        @DefaultBoolean(true)
        boolean passthroughMotd();

        @Comment("""
            Maximum amount of players that can connect.
            This is only visual, and is only applied if passthrough-motd is disabled.""")
        @DefaultNumeric(100)
        int maxPlayers();

        @Comment("Whether to relay the player count and max players from the Java server to Bedrock players.")
        @DefaultBoolean(true)
        boolean passthroughPlayerCounts();

        @Comment("""
            Whether to use server API methods to determine the Java server's MOTD and ping passthrough.
            There is no need to disable this unless your MOTD or player count does not appear properly.""")
        @DefaultBoolean(true)
        @PluginSpecific
        boolean integratedPingPassthrough();

        @Comment("How often to ping the Java server to refresh MOTD and player count, in seconds.")
        @DefaultNumeric(3)
        int pingPassthroughInterval();
    }

    @ConfigSerializable
    interface GameplayConfig {

        @Comment("The server name that will be sent to Minecraft: Bedrock Edition clients. This is visible in both the pause menu and the settings menu.")
        @DefaultString("Geyser")
        String serverName();

        @Comment("""
            Allow a fake cooldown indicator to be sent. Bedrock players otherwise do not see a cooldown as they still use 1.8 combat.
            Please note: if the cooldown is enabled, some users may see a black box during the cooldown sequence, like below:
            https://geysermc.org/img/external/cooldown_indicator.png
            This can be disabled by going into Bedrock settings under the accessibility tab and setting "Text Background Opacity" to 0
            This setting can be set to "title", "actionbar" or "disabled\"""")
        default CooldownUtils.CooldownType showCooldown() {
            return CooldownUtils.CooldownType.TITLE;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        @Comment("""
            Bedrock clients can freeze when opening up the command prompt for the first time if given a lot of commands.
            Disabling this will prevent command suggestions from being sent and solve freezing for Bedrock clients.""")
        @DefaultBoolean(true)
        boolean commandSuggestions();

        @Comment("Controls if coordinates are shown to players.")
        @DefaultBoolean(true)
        boolean showCoordinates();

        @Comment("Whether Bedrock players are blocked from performing their scaffolding-style bridging.")
        boolean disableBedrockScaffolding();

        @Comment("""
            Bedrock prevents building and displaying blocks above Y127 in the Nether.
            This config option works around that by changing the Nether dimension ID to the End ID.
            The main downside to this is that the entire Nether will have the same red fog rather than having different fog for each biome.""")
        boolean netherRoofWorkaround();

        @Comment("""
            Whether to show Bedrock Edition emotes to other Bedrock Edition players.
            """)
        @DefaultBoolean(true)
        boolean emotesEnabled();

        @Comment("""
            Which item to use to mark unavailable slots in a Bedrock player inventory. Examples of this are the 2x2 crafting grid while in creative,
            or custom inventory menus with sizes different from the usual 3x9. A barrier block is the default item.
            This config option can be set to any Bedrock item identifier. If you want to set this to a custom item, make sure that you specify the item in the following format: "geyser_custom:<mapping-name>"
            """)
        @DefaultString("minecraft:barrier")
        String unusableSpaceBlock();

        @Comment("""
            Whether to add any items and blocks which normally does not exist in Bedrock Edition.
            This should only need to be disabled if using a proxy that does not use the "transfer packet" style of server switching.
            If this is disabled, furnace minecart items will be mapped to hopper minecart items.
            Geyser's block, item, and skull mappings systems will also be disabled.
            This option requires a restart of Geyser in order to change its setting.""")
        @DefaultBoolean(true)
        boolean enableCustomContent();

        @Comment("""
            Force clients to load all resource packs if there are any.
            If set to false, it allows the user to connect to the server even if they don't
            want to download the resource packs.""")
        @DefaultBoolean(true)
        boolean forceResourcePacks();

        @Comment("""
            Whether to automatically serve a resource pack that is required for some Geyser features to all connecting Bedrock players.
            If enabled, force-resource-packs will be enabled.""")
        @DefaultBoolean(true)
        boolean enableIntegratedPack();

        @Comment("""
            Whether to forward player ping to the server. While enabling this will allow Bedrock players to have more accurate
            ping, it may also cause players to time out more easily.""")
        boolean forwardPlayerPing();

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        @Comment("""
            Allows Xbox achievements to be unlocked.
            If a player types in an unknown command, they will receive a message that states cheats are disabled.
            Otherwise, commands work as expected.""")
        boolean xboxAchievementsEnabled();

        @Comment("""
            The maximum number of custom skulls to be displayed per player. Increasing this may decrease performance on weaker devices.
            A value of 0 will disable all custom skulls.
            Setting this to -1 will cause all custom skulls to be displayed regardless of distance or number.""")
        @DefaultNumeric(128)
        int maxVisibleCustomSkulls();

        @Comment("The radius in blocks around the player in which custom skulls are displayed.")
        @DefaultNumeric(32)
        int customSkullRenderDistance();
    }

    @ConfigSerializable
    interface AdvancedBedrockConfig {
        @Comment("""
                The port to broadcast to Bedrock clients with the MOTD that they should use to connect to the server.
                A value of 0 will broadcast the port specified above.
                DO NOT change this unless Geyser runs on a different port than the one that is used to connect.""")
        @DefaultNumeric(0)
        @NumericRange(from = 0, to = 65535)
        int broadcastPort();

        void broadcastPort(int port);

        @Comment("""
                How much to compress network traffic to the Bedrock client. The higher the number, the more CPU usage used, but
                the smaller the bandwidth used. Does not have any effect below -1 or above 9. Set to -1 to disable.""")
        @DefaultNumeric(6)
        @NumericRange(from = -1, to = 9)
        int compressionLevel();

        @Comment("""
                Whether to expect HAPROXY protocol for connecting Bedrock clients.
                This is useful only when you are running a UDP reverse proxy in front of your Geyser instance.
                IF YOU DON'T KNOW WHAT THIS IS, DON'T TOUCH IT!""")
        @DefaultBoolean
        boolean useHaproxyProtocol();

        @Comment("""
                A list of allowed HAPROXY protocol speaking proxy IP addresses/subnets. Only effective when "use-proxy-protocol" is enabled, and
                should really only be used when you are not able to use a proper firewall (usually true with shared hosting providers etc.).
                Keeping this list empty means there is no IP address whitelist.
                IP addresses, subnets, and links to plain text files are supported.""")
        default List<String> haproxyProtocolWhitelistedIps() {
            return Collections.emptyList();
        }

        @Comment("""
            The internet supports a maximum MTU of 1492 but could cause issues with packet fragmentation.
            1400 is the default.""")
        @DefaultNumeric(1400)
        int mtu();

        @Comment("""
            This option disables the auth step Geyser performs for connecting Bedrock players.
            It can be used to allow connections from ProxyPass and WaterdogPE. In these cases, make sure that users
            cannot directly connect to this Geyser instance. See https://www.spigotmc.org/wiki/firewall-guide/ for
            assistance - and use UDP instead of TCP.
            Disabling Bedrock authentication for other use-cases is NOT SUPPORTED, as it allows anyone to spoof usernames, and is therefore a security risk.
            All Floodgate functionality (including skin uploading and account linking) will also not work when this option is disabled.""")
        @DefaultBoolean(true)
        boolean validateBedrockLogin();
    }

    @ConfigSerializable
    interface AdvancedJavaConfig {
        @Comment("""
                Whether to enable HAPROXY protocol when connecting to the Java server.
                This is useful only when:
                1) Your Java server supports HAPROXY protocol (it probably doesn't)
                2) You run Velocity or BungeeCord with the option enabled in the proxy's main config.
                IF YOU DON'T KNOW WHAT THIS IS, DON'T TOUCH IT!""")
        boolean useHaproxyProtocol();

        @Comment("""
        Whether to connect directly into the Java server without creating a TCP connection.
        This should only be disabled if a plugin that interfaces with packets or the network does not work correctly with Geyser.
        If enabled, the remote address and port sections are ignored.
        If disabled, expect performance decrease and latency increase.
        """)
        @DefaultBoolean(true)
        @PluginSpecific
        boolean useDirectConnection();

        @Comment("""
        Whether Geyser should attempt to disable packet compression (from the Java Server to Geyser) for Bedrock players.
        This should be a benefit as there is no need to compress data when Java packets aren't being handled over the network.
        This requires use-direct-connection to be true.
        """)
        @DefaultBoolean(true)
        @PluginSpecific
        boolean disableCompression();
    }

    @ConfigSerializable
    interface AdvancedConfig {
        @Comment("""
            Specify how many days player skin images will be cached to disk to save downloading them from the internet.
            A value of 0 is disabled. (Default: 0)""")
        int cacheImages();

        @Comment("""
            Geyser updates the Scoreboard after every Scoreboard packet, but when Geyser tries to handle
            a lot of scoreboard packets per second, this can cause serious lag.
            This option allows you to specify after how many Scoreboard packets per seconds
            the Scoreboard updates will be limited to four updates per second.""")
        @DefaultNumeric(20)
        int scoreboardPacketThreshold();

        @Comment("""
            Whether Geyser should send team names in command suggestions.
            Disable this if you have a lot of teams used that you don't need as suggestions.""")
        @DefaultBoolean(true)
        boolean addTeamSuggestions();

        @Comment("""
            A list of remote resource pack urls to send to the Bedrock client for downloading.
            The Bedrock client is very picky about how these are delivered - please see our wiki page for further info: https://geysermc.org/wiki/geyser/packs/
            """)
        default List<String> resourcePackUrls() {
            return Collections.emptyList();
        }

        // Cannot be type File yet because we may want to hide it in plugin instances.
        @Comment("""
            Floodgate uses encryption to ensure use from authorized sources.
            This should point to the public key generated by Floodgate (BungeeCord, Spigot or Velocity)
            You can ignore this when not using Floodgate.
            If you're using a plugin version of Floodgate on the same server, the key will automatically be picked up from Floodgate.""")
        @DefaultString("key.pem")
        String floodgateKeyFile();

        @Comment("Advanced networking options for the Geyser to Java server connection")
        AdvancedJavaConfig java();

        @Comment("Advanced networking options for Geyser's Bedrock listener")
        AdvancedBedrockConfig bedrock();
    }
}
