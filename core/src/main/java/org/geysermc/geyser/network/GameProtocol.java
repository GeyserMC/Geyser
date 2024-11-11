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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v671.Bedrock_v671;
import org.cloudburstmc.protocol.bedrock.codec.v685.Bedrock_v685;
import org.cloudburstmc.protocol.bedrock.codec.v686.Bedrock_v686;
import org.cloudburstmc.protocol.bedrock.codec.v712.Bedrock_v712;
import org.cloudburstmc.protocol.bedrock.codec.v729.Bedrock_v729;
import org.cloudburstmc.protocol.bedrock.codec.v748.Bedrock_v748;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.codec.PacketCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Contains information about the supported protocols in Geyser.
 */
public final class GameProtocol {

    /**
     * Default Bedrock codec that should act as a fallback. Should represent the latest available
     * release of the game that Geyser supports.
     */
    public static final BedrockCodec DEFAULT_BEDROCK_CODEC = CodecProcessor.processCodec(Bedrock_v748.CODEC.toBuilder()
        .minecraftVersion("1.21.44")
        .build());

    /**
     * A list of all supported Bedrock versions that can join Geyser
     */
    public static final List<BedrockCodec> SUPPORTED_BEDROCK_CODECS = new ArrayList<>();

    /**
     * Java codec that is supported. We only ever support one version for
     * Java Edition.
     */
    private static final PacketCodec DEFAULT_JAVA_CODEC = MinecraftCodec.CODEC;

    static {
        SUPPORTED_BEDROCK_CODECS.add(CodecProcessor.processCodec(Bedrock_v671.CODEC.toBuilder()
            .minecraftVersion("1.20.80/1.20.81")
            .build()));
        SUPPORTED_BEDROCK_CODECS.add(CodecProcessor.processCodec(Bedrock_v685.CODEC.toBuilder()
            .minecraftVersion("1.21.0/1.21.1")
            .build()));
        SUPPORTED_BEDROCK_CODECS.add(CodecProcessor.processCodec(Bedrock_v686.CODEC.toBuilder()
            .minecraftVersion("1.21.2/1.21.3")
            .build()));
        SUPPORTED_BEDROCK_CODECS.add(CodecProcessor.processCodec(Bedrock_v712.CODEC.toBuilder()
            .minecraftVersion("1.21.20 - 1.21.23")
            .build()));
        SUPPORTED_BEDROCK_CODECS.add(CodecProcessor.processCodec(Bedrock_v729.CODEC.toBuilder()
            .minecraftVersion("1.21.30/1.21.31")
            .build()));
        SUPPORTED_BEDROCK_CODECS.add(DEFAULT_BEDROCK_CODEC.toBuilder()
            .minecraftVersion("1.21.40 - 1.21.44")
            .build());
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

    public static boolean isPre1_21_0(GeyserSession session) {
        return session.getUpstream().getProtocolVersion() < Bedrock_v685.CODEC.getProtocolVersion();
    }

    public static boolean isPre1_21_2(GeyserSession session) {
        return session.getUpstream().getProtocolVersion() < Bedrock_v686.CODEC.getProtocolVersion();
    }

    public static boolean isPre1_21_30(GeyserSession session) {
        return session.getUpstream().getProtocolVersion() < Bedrock_v729.CODEC.getProtocolVersion();
    }

    public static boolean isPre1_21_40(GeyserSession session) {
        return session.getUpstream().getProtocolVersion() < Bedrock_v748.CODEC.getProtocolVersion();
    }

    /**
     * Gets the {@link PacketCodec} for Minecraft: Java Edition.
     *
     * @return the packet codec for Minecraft: Java Edition
     */
    public static PacketCodec getJavaCodec() {
        return DEFAULT_JAVA_CODEC;
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
        StringJoiner joiner = new StringJoiner(", ");
        for (BedrockCodec packetCodec : SUPPORTED_BEDROCK_CODECS) {
            joiner.add(packetCodec.getMinecraftVersion());
        }

        return joiner.toString();
    }

    /**
     * @return a string showing all supported Java versions for this Geyser instance
     */
    public static String getAllSupportedJavaVersions() {
        StringJoiner joiner = new StringJoiner(", ");
        for (String version : getJavaVersions()) {
            joiner.add(version);
        }

        return joiner.toString();
    }

    private GameProtocol() {
    }
}
