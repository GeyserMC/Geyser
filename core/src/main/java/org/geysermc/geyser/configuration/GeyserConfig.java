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
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.api.network.BedrockListener;
import org.geysermc.geyser.api.network.RemoteServer;
import org.geysermc.geyser.network.GameProtocol;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultBoolean;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultNumeric;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultString;
import org.spongepowered.configurate.interfaces.meta.range.NumericRange;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface GeyserConfig {
    BedrockConfig bedrock();

    JavaConfig java();

    Path floodgateKeyPath();

    @Comment("""
            For online mode authentication type only.
            Stores a list of Bedrock players that should have their Java Edition account saved after login.
            This saves a token that can be reused to authenticate the player later. This does not save emails or passwords,
            but you should still be cautious when adding to this list and giving others access to this Geyser instance's files.
            Removing a name from this list will delete its cached login information on the next Geyser startup.
            The file that tokens will be saved in is in the same folder as this config, named "saved-refresh-tokens.json".""")
    default List<String> savedUserLogins() {
        return List.of("ThisExampleUsernameShouldBeLongEnoughToNeverBeAnXboxUsername",
                "ThisOtherExampleUsernameShouldAlsoBeLongEnough");
    }

    @Comment("""
            Specify how many seconds to wait while user authorizes Geyser to access their Microsoft account.
            User is allowed to disconnect from the server during this period.""")
    @DefaultNumeric(128)
    int pendingAuthenticationTimeout();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @Comment("""
            Bedrock clients can freeze when opening up the command prompt for the first time if given a lot of commands.
            Disabling this will prevent command suggestions from being sent and solve freezing for Bedrock clients.""")
    @DefaultBoolean(true)
    boolean commandSuggestions();

    @Comment("Relay the MOTD from the Java server to Bedrock players.")
    @DefaultBoolean(true)
    boolean passthroughMotd();

    @Comment("Relay the player count and max players from the Java server to Bedrock players.")
    @DefaultBoolean(true)
    boolean passthroughPlayerCounts();

    @Comment("How often to ping the Java server to refresh MOTD and player count, in seconds.")
    @DefaultNumeric(3)
    int pingPassthroughInterval();

    @Comment("""
            Whether to forward player ping to the server. While enabling this will allow Bedrock players to have more accurate
            ping, it may also cause players to time out more easily.""")
    boolean forwardPlayerPing();

    @Comment("""
            Maximum amount of players that can connect.
            This is only visual, and is only applied if passthrough-motd is disabled.""")
    @DefaultNumeric(100)
    int maxPlayers();

    @Comment("If debug messages should be sent through console")
    boolean debugMode();

    @Comment("""
            Allow a fake cooldown indicator to be sent. Bedrock players otherwise do not see a cooldown as they still use 1.8 combat.
            Please note: if the cooldown is enabled, some users may see a black box during the cooldown sequence, like below:
            https://cdn.discordapp.com/attachments/613170125696270357/957075682230419466/Screenshot_from_2022-03-25_20-35-08.png
            This can be disabled by going into Bedrock settings under the accessibility tab and setting "Text Background Opacity" to 0
            This setting can be set to "title", "actionbar" or "false\"""")
    @DefaultString("title")
    String showCooldown();

    @Comment("Controls if coordinates are shown to players.")
    @DefaultBoolean(true)
    boolean showCoordinates();

    @Comment("Whether Bedrock players are blocked from performing their scaffolding-style bridging.")
    boolean disableBedrockScaffolding();

    //@DefaultString("disabled")
    EmoteOffhandWorkaroundOption emoteOffhandWorkaround();

    String defaultLocale();

    @Comment("""
            Specify how many days images will be cached to disk to save downloading them from the internet.
            A value of 0 is disabled. (Default: 0)""")
    int cacheImages();

    @Comment("Allows custom skulls to be displayed. Keeping them enabled may cause a performance decrease on older/weaker devices.")
    @DefaultBoolean(true)
    boolean allowCustomSkulls();

    @Comment("""
            The maximum number of custom skulls to be displayed per player. Increasing this may decrease performance on weaker devices.
            Setting this to -1 will cause all custom skulls to be displayed regardless of distance or number.""")
    @DefaultNumeric(128)
    int maxVisibleCustomSkulls();

    @Comment("The radius in blocks around the player in which custom skulls are displayed.")
    @DefaultNumeric(32)
    int customSkullRenderDistance();

    @Comment("""
            Whether to add any items and blocks which normally does not exist in Bedrock Edition.
            This should only need to be disabled if using a proxy that does not use the "transfer packet" style of server switching.
            If this is disabled, furnace minecart items will be mapped to hopper minecart items.
            Geyser's block, item, and skull mappings systems will also be disabled.
            This option requires a restart of Geyser in order to change its setting.""")
    @DefaultBoolean(true)
    boolean addNonBedrockItems();

    @Comment("""
            Bedrock prevents building and displaying blocks above Y127 in the Nether.
            This config option works around that by changing the Nether dimension ID to the End ID.
            The main downside to this is that the entire Nether will have the same red fog rather than having different fog for each biome.""")
    boolean aboveBedrockNetherBuilding();

    @Comment("""
            Force clients to load all resource packs if there are any.
            If set to false, it allows the user to connect to the server even if they don't
            want to download the resource packs.""")
    @DefaultBoolean(true)
    boolean forceResourcePacks();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @Comment("""
            Allows Xbox achievements to be unlocked.
            THIS DISABLES ALL COMMANDS FROM SUCCESSFULLY RUNNING FOR BEDROCK IN-GAME, as otherwise Bedrock thinks you are cheating.""")
    boolean xboxAchievementsEnabled();

    @Comment("Whether player IP addresses will be logged by the server.")
    @DefaultBoolean(true)
    boolean logPlayerIpAddresses();

    @Comment("""
            Whether to alert the console and operators that a new Geyser version is available that supports a Bedrock version
            that this Geyser version does not support. It's recommended to keep this option enabled, as many Bedrock platforms
            auto-update.""")
    @DefaultBoolean(true)
    boolean notifyOnNewBedrockUpdate();

    @Comment("""
            Which item to use to mark unavailable slots in a Bedrock player inventory. Examples of this are the 2x2 crafting grid while in creative,
            or custom inventory menus with sizes different from the usual 3x9. A barrier block is the default item.""")
    @DefaultString("minecraft:barrier")
    String unusableSpaceBlock();

    @Comment("""
            bStats is a stat tracker that is entirely anonymous and tracks only basic information
            about Geyser, such as how many people are online, how many servers are using Geyser,
            what OS is being used, etc. You can learn more about bStats here: https://bstats.org/.
            https://bstats.org/plugin/server-implementation/GeyserMC""")
    MetricsInfo metrics();

    @ConfigSerializable
    interface BedrockConfig extends BedrockListener {
        @Override
        @Comment("""
                The IP address that will listen for connections.
                Generally, you should only uncomment and change this if you want to limit what IPs can connect to your server.""")
        @NonNull
        @DefaultString("0.0.0.0")
        String address();

        @Override
        @Comment("The port that will listen for connections")
        @DefaultNumeric(19132)
        int port();

        @Override
        @Comment("""
                The port to broadcast to Bedrock clients with the MOTD that they should use to connect to the server.
                DO NOT uncomment and change this unless Geyser runs on a different internal port than the one that is used to connect.""")
        @DefaultNumeric(19132)
        int broadcastPort();

        void address(String address);

        void port(int port);

        void broadcastPort(int broadcastPort);

        @Override
        @DefaultString("Geyser")
        String primaryMotd();

        @Override
        @DefaultString("Another Geyser server.") // TODO migrate or change name
        String secondaryMotd();

        @Comment("""
                How much to compress network traffic to the Bedrock client. The higher the number, the more CPU usage used, but
                the smaller the bandwidth used. Does not have any effect below -1 or above 9. Set to -1 to disable.""")
        @DefaultNumeric(6)
        @NumericRange(from = -1, to = 9)
        int compressionLevel();

        @DefaultBoolean
        boolean enableProxyProtocol();

        @Comment("""
                A list of allowed PROXY protocol speaking proxy IP addresses/subnets. Only effective when "enable-proxy-protocol" is enabled, and
                should really only be used when you are not able to use a proper firewall (usually true with shared hosting providers etc.).
                Keeping this list empty means there is no IP address whitelist.
                IP addresses, subnets, and links to plain text files are supported.""")
        List<String> proxyProtocolWhitelistedIPs();

//        /**
//         * @return Unmodifiable list of {@link CIDRMatcher}s from {@link #proxyProtocolWhitelistedIPs()}
//         */
//        @Exclude
//        List<CIDRMatcher> whitelistedIPsMatchers();
    }

    @ConfigSerializable
    interface JavaConfig extends RemoteServer {

        void address(String address);

        void port(int port);

        @Override
        @Comment("""
                What type of authentication Bedrock players will be checked against when logging into the Java server.
                Can be floodgate (see https://wiki.geysermc.org/floodgate/), online, or offline.""")
        @NonNull
        default AuthType authType() {
            return AuthType.ONLINE;
        }

        @Comment("""
                Whether to enable PROXY protocol or not while connecting to the server.
                This is useful only when:
                1) Your server supports PROXY protocol (it probably doesn't)
                2) You run Velocity or BungeeCord with the option enabled in the proxy's main config.
                IF YOU DON'T KNOW WHAT THIS IS, DON'T TOUCH IT!""")
        boolean useProxyProtocol();

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

        void authType(AuthType authType);
    }

    @ConfigSerializable
    interface MetricsInfo {

        @DefaultBoolean(true)
        boolean enabled();

        default UUID uniqueId() { //TODO rename?
            return UUID.randomUUID();
        }
    }

    @Comment("""
            Geyser updates the Scoreboard after every Scoreboard packet, but when Geyser tries to handle
            a lot of scoreboard packets per second can cause serious lag.
            This option allows you to specify after how many Scoreboard packets per seconds
            the Scoreboard updates will be limited to four updates per second.""")
    @DefaultNumeric(20)
    int scoreboardPacketThreshold();

    @Comment("""
            Allow connections from ProxyPass and Waterdog.
            See https://www.spigotmc.org/wiki/firewall-guide/ for assistance - use UDP instead of TCP.""")
    // if u have offline mode enabled pls be safe
    boolean enableProxyConnections();

    @Comment("""
            The internet supports a maximum MTU of 1492 but could cause issues with packet fragmentation.
            1400 is the default.""")
    @DefaultNumeric(1400)
    int mtu();

    @Comment("Do not change!")
    default int version() {
        return Constants.CONFIG_VERSION;
    }
}
