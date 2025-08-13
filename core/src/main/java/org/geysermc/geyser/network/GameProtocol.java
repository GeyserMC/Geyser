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
import org.cloudburstmc.protocol.bedrock.codec.v786.Bedrock_v786;
import org.cloudburstmc.protocol.bedrock.codec.v800.Bedrock_v800;
import org.cloudburstmc.protocol.bedrock.codec.v818.Bedrock_v818;
import org.cloudburstmc.protocol.bedrock.codec.v819.Bedrock_v819;
import org.cloudburstmc.protocol.bedrock.codec.v827.Bedrock_v827;
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
        register(Bedrock_v786.CODEC, "1.21.70", "1.21.71", "1.21.72", "1.21.73");
        register(Bedrock_v800.CODEC, "1.21.80", "1.21.81", "1.21.82", "1.21.83", "1.21.84");
        register(Bedrock_v818.CODEC, "1.21.90", "1.21.91", "1.21.92");
        register(Bedrock_v819.CODEC, "1.21.93", "1.21.94");
        register(Bedrock_v827.CODEC, "1.21.100");

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

    public static boolean isTheOneVersionWithBrokenForms(GeyserSession session) {
        return session.protocolVersion() == Bedrock_v786.CODEC.getProtocolVersion();
    }

    public static boolean is1_21_80orHigher(GeyserSession session) {
        return session.protocolVersion() >= Bedrock_v800.CODEC.getProtocolVersion();
    }

    public static boolean is1_21_90orHigher(GeyserSession session) {
        return session.protocolVersion() >= Bedrock_v818.CODEC.getProtocolVersion();
    }

    public static boolean is1_21_80(GeyserSession session) {
        return session.protocolVersion() == Bedrock_v800.CODEC.getProtocolVersion();
    }

    /**
     * Gets the supported Minecraft: Java Edition version names.
     *
     * @return the supported Minecraft: Java Edition version names
     */
    public static List<String> getJavaVersions() {
        return List.of(DEFAULT_JAVA_CODEC.getMinecraftVersion(), "1.21.8");
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
        return "1.21.8";
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
