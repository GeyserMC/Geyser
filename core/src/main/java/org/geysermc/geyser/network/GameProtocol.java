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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v1001.Bedrock_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v589.Bedrock_v589;
import org.cloudburstmc.protocol.bedrock.codec.v594.Bedrock_v594;
import org.cloudburstmc.protocol.bedrock.codec.v618.Bedrock_v618;
import org.cloudburstmc.protocol.bedrock.codec.v622.Bedrock_v622;
import org.cloudburstmc.protocol.bedrock.codec.v630.Bedrock_v630;
import org.cloudburstmc.protocol.bedrock.codec.v649.Bedrock_v649;
import org.cloudburstmc.protocol.bedrock.codec.v662.Bedrock_v662;
import org.cloudburstmc.protocol.bedrock.codec.v671.Bedrock_v671;
import org.cloudburstmc.protocol.bedrock.codec.v685.Bedrock_v685;
import org.cloudburstmc.protocol.bedrock.codec.v686.Bedrock_v686;
import org.cloudburstmc.protocol.bedrock.codec.v712.Bedrock_v712;
import org.cloudburstmc.protocol.bedrock.codec.v729.Bedrock_v729;
import org.cloudburstmc.protocol.bedrock.codec.v748.Bedrock_v748;
import org.cloudburstmc.protocol.bedrock.codec.v766.Bedrock_v766;
import org.cloudburstmc.protocol.bedrock.codec.v776.Bedrock_v776;
import org.cloudburstmc.protocol.bedrock.codec.v786.Bedrock_v786;
import org.cloudburstmc.protocol.bedrock.codec.v800.Bedrock_v800;
import org.cloudburstmc.protocol.bedrock.codec.v818.Bedrock_v818;
import org.cloudburstmc.protocol.bedrock.codec.v819.Bedrock_v819;
import org.cloudburstmc.protocol.bedrock.codec.v827.Bedrock_v827;
import org.cloudburstmc.protocol.bedrock.codec.v844.Bedrock_v844;
import org.cloudburstmc.protocol.bedrock.codec.v859.Bedrock_v859;
import org.cloudburstmc.protocol.bedrock.codec.v860.Bedrock_v860;
import org.cloudburstmc.protocol.bedrock.codec.v898.Bedrock_v898;
import org.cloudburstmc.protocol.bedrock.codec.v924.Bedrock_v924;
import org.cloudburstmc.protocol.bedrock.codec.v944.Bedrock_v944;
import org.cloudburstmc.protocol.bedrock.codec.v975.Bedrock_v975;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.geysermc.geyser.api.util.MinecraftVersion;
import org.geysermc.geyser.impl.MinecraftVersionImpl;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.codec.PacketCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains information about the supported protocols in Geyser.
 */
public final class GameProtocol {

    /**
     * All Bedrock protocol codecs that Geyser uses
     */
    static final List<BedrockCodec> SUPPORTED_BEDROCK_CODECS = new ArrayList<>();

    /**
     * All bedrock protocol versions that Geyser supports
     */
    public static final IntList SUPPORTED_BEDROCK_PROTOCOLS = new IntArrayList();

    /**
     * All bedrock minecraft versions that Geyser supports.
     * There may be multiple MinecraftVersions with the same protocol version.
     */
    public static final List<MinecraftVersion> SUPPORTED_BEDROCK_VERSIONS = new ArrayList<>();

    /**
     * The latest Bedrock protocol version that Geyser supports.
     */
    public static final int DEFAULT_BEDROCK_PROTOCOL;

    /**
     * The latest Bedrock Minecraft version that Geyser supports.
     */
    public static final String DEFAULT_BEDROCK_VERSION;

    /**
     * Java codec that is supported. We only ever support one version for
     * Java Edition.
     */
    private static final PacketCodec DEFAULT_JAVA_CODEC = MinecraftCodec.CODEC;

    static {
        // Strict ordering
        // Legacy Bedrock (pre-26.0): reuse nearest known palettes and remap unavailable content.
        register(Bedrock_v589.CODEC, "1.20.0", "1.20.1");
        register(Bedrock_v594.CODEC, "1.20.10", "1.20.15");
        register(Bedrock_v618.CODEC, "1.20.30", "1.20.32");
        register(Bedrock_v622.CODEC, "1.20.40", "1.20.41");
        register(Bedrock_v630.CODEC, "1.20.50", "1.20.51");
        register(Bedrock_v649.CODEC, "1.20.60", "1.20.61", "1.20.62");
        register(Bedrock_v662.CODEC, "1.20.70", "1.20.71", "1.20.72", "1.20.73");
        register(Bedrock_v671.CODEC, "1.20.80", "1.20.81");
        register(Bedrock_v685.CODEC, "1.21.0");
        register(Bedrock_v686.CODEC, "1.21.2");
        register(Bedrock_v712.CODEC, "1.21.20");
        register(Bedrock_v729.CODEC, "1.21.30");
        register(Bedrock_v748.CODEC, "1.21.40");
        register(Bedrock_v766.CODEC, "1.21.50");
        register(Bedrock_v776.CODEC, "1.21.60", "1.21.61", "1.21.62");
        register(Bedrock_v786.CODEC, "1.21.70");
        register(Bedrock_v800.CODEC, "1.21.80");
        register(Bedrock_v818.CODEC, "1.21.90", "1.21.91", "1.21.92");
        register(Bedrock_v819.CODEC, "1.21.93", "1.21.94");
        register(Bedrock_v827.CODEC, "1.21.100", "1.21.101");
        register(Bedrock_v844.CODEC, "1.21.111", "1.21.112", "1.21.113", "1.21.114");
        register(Bedrock_v859.CODEC, "1.21.120", "1.21.121", "1.21.122", "1.21.123");
        register(Bedrock_v860.CODEC, "1.21.124");
        register(Bedrock_v898.CODEC, "1.21.130", "1.21.131", "1.21.132");
        register(Bedrock_v924.CODEC, "26.0", "26.1", "26.2", "26.3");
        register(Bedrock_v944.CODEC, "26.10");
        register(Bedrock_v975.CODEC, "26.20", "26.21", "26.22", "26.23");
        register(Bedrock_v1001.CODEC, "26.30", "26.31", "26.32", "26.33");

        MinecraftVersion latestBedrock = SUPPORTED_BEDROCK_VERSIONS.getLast();
        DEFAULT_BEDROCK_VERSION = latestBedrock.versionString();
        DEFAULT_BEDROCK_PROTOCOL = latestBedrock.protocolVersion();
    }

    /**
     * Registers a bedrock codec, along with its protocol version and minecraft version(s).
     * This method must be called in ascending order in terms of protocol version.
     *
     * @param codec the codec to register
     * @param minecraftVersions all versions the codec supports, in ascending order
     */
    private static void register(BedrockCodec codec, String... minecraftVersions) {
        // modify packet serializers to better fit our use
        codec = CodecProcessor.processCodec(codec);

        SUPPORTED_BEDROCK_CODECS.add(codec);
        SUPPORTED_BEDROCK_PROTOCOLS.add(codec.getProtocolVersion());

        for (String version : minecraftVersions) {
            SUPPORTED_BEDROCK_VERSIONS.add(new MinecraftVersionImpl(version, codec.getProtocolVersion()));
        }
    }

    /**
     * Registers a bedrock codec, its protocol version, and a single minecraft version which is taken from the codec.
     * This method must be called in ascending order in terms of protocol version.
     *
     * @param codec the codec to register
     */
    private static void register(BedrockCodec codec) {
        register(codec, codec.getMinecraftVersion());
    }

    /**
     * Gets the {@link BedrockPacketCodec} of the given protocol version.
     * @param protocolVersion The protocol version to attempt to find
     * @return The packet codec, or null if the client's protocol is unsupported
     */
    public static @Nullable BedrockCodec getBedrockCodec(int protocolVersion) {
        for (BedrockCodec packetCodec : SUPPORTED_BEDROCK_CODECS) {
            if (packetCodec.getProtocolVersion() == protocolVersion) {
                return packetCodec;
            }
        }
        return null;
    }

    /* Bedrock convenience methods to gatekeep features and easily remove the check on version removal */

    public static boolean is1_20_0orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v589.CODEC.getProtocolVersion();
    }

    public static boolean is1_20_70orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v662.CODEC.getProtocolVersion();
    }

    public static boolean is1_21_0orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v685.CODEC.getProtocolVersion();
    }

    public static boolean is1_21_50orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v766.CODEC.getProtocolVersion();
    }

    /**
     * Bedrock 1.21.70 (786): Spring to Life — climate variants for cow/pig/chicken/egg.
     */
    public static boolean is1_21_70orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v786.CODEC.getProtocolVersion();
    }

    /**
     * Bedrock 1.21.60 (776) rewrote creative inventory + moved the item registry out of StartGame
     * into a full ItemComponentPacket. Older clients need StartGame item entries and must not receive
     * the modern full ItemComponentPacket (crashes as "broken packet", e.g. 1.20.62).
     */
    public static boolean isPreCreativeInventoryRewrite(int protocolVersion) {
        return protocolVersion < Bedrock_v776.CODEC.getProtocolVersion();
    }

    public static boolean is1_21_80orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v800.CODEC.getProtocolVersion();
    }

    /**
     * Exactly Bedrock 1.21.80 (protocol 800). Drop 2 / locator bar were still experiments here.
     */
    public static boolean is1_21_80(int protocolVersion) {
        return protocolVersion == Bedrock_v800.CODEC.getProtocolVersion();
    }

    public static boolean is1_21_90orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v818.CODEC.getProtocolVersion();
    }

    public static boolean is1_21_110orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v844.CODEC.getProtocolVersion();
    }

    public static boolean is1_21_130orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v898.CODEC.getProtocolVersion();
    }

    public static boolean is26_0orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v924.CODEC.getProtocolVersion();
    }

    public static boolean is26_10orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v944.CODEC.getProtocolVersion();
    }

    public static boolean is26_20orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v975.CODEC.getProtocolVersion();
    }

    public static boolean is26_30orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v1001.CODEC.getProtocolVersion();
    }

    /**
     * Gets the supported Minecraft: Java Edition version names.
     *
     * @return the supported Minecraft: Java Edition version names
     */
    public static List<String> getJavaVersions() {
        return List.of(DEFAULT_JAVA_CODEC.getMinecraftVersion(), "26.1.1", "26.1.2");
    }

    /**
     * Gets the supported Minecraft: Java Edition protocol version.
     *
     * @return the supported Minecraft: Java Edition protocol version
     */
    public static int getJavaProtocolVersion() {
        return DEFAULT_JAVA_CODEC.getProtocolVersion();
    }

    /**
     * Gets the supported Minecraft: Java Edition version.
     *
     * @return the supported Minecraft: Java Edition version
     */
    public static String getJavaMinecraftVersion() {
        return DEFAULT_JAVA_CODEC.getMinecraftVersion();
    }

    /**
     * @return a string showing all supported Bedrock versions for this Geyser instance
     */
    public static String getAllSupportedBedrockVersions() {
        return SUPPORTED_BEDROCK_VERSIONS.stream()
            .map(MinecraftVersion::versionString)
            .collect(Collectors.joining(", "));
    }

    /**
     * @return a string showing all supported Java versions for this Geyser instance
     */
    public static String getAllSupportedJavaVersions() {
        return String.join(", ", getJavaVersions());
    }

    private GameProtocol() {
    }
}
