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
import org.cloudburstmc.protocol.bedrock.codec.v844.Bedrock_v844;
import org.cloudburstmc.protocol.bedrock.codec.v859.Bedrock_v859;
import org.cloudburstmc.protocol.bedrock.codec.v860.Bedrock_v860;
import org.cloudburstmc.protocol.bedrock.codec.v898.Bedrock_v898;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.geysermc.geyser.api.util.MinecraftVersion;
import org.geysermc.geyser.impl.MinecraftVersionImpl;
import org.geysermc.geyser.session.GeyserSession;
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
    private static final List<BedrockCodec> SUPPORTED_BEDROCK_CODECS = new ArrayList<>();

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
        register(Bedrock_v844.CODEC, "1.21.111", "1.21.112", "1.21.113", "1.21.114");
        register(Bedrock_v859.CODEC, "1.21.120", "1.21.121", "1.21.122", "1.21.123");
        register(Bedrock_v860.CODEC);
        register(Bedrock_v898.CODEC);

        MinecraftVersion latestBedrock = SUPPORTED_BEDROCK_VERSIONS.get(SUPPORTED_BEDROCK_VERSIONS.size() - 1);
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

    public static boolean is1_21_110orHigher(GeyserSession session) {
        return is1_21_110orHigher(session.protocolVersion());
    }

    public static boolean is1_21_110orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v844.CODEC.getProtocolVersion();
    }

    public static boolean is1_21_130orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v898.CODEC.getProtocolVersion();
    }

    /**
     * Gets the supported Minecraft: Java Edition version names.
     *
     * @return the supported Minecraft: Java Edition version names
     */
    public static List<String> getJavaVersions() {
        return List.of(DEFAULT_JAVA_CODEC.getMinecraftVersion());
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
