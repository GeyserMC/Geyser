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

import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.codec.PacketCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v589.Bedrock_v589;
import org.cloudburstmc.protocol.bedrock.codec.v594.Bedrock_v594;
import org.cloudburstmc.protocol.bedrock.codec.v618.Bedrock_v618;
import org.cloudburstmc.protocol.bedrock.codec.v622.Bedrock_v622;
import org.cloudburstmc.protocol.bedrock.codec.v630.Bedrock_v630;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.geysermc.geyser.session.GeyserSession;

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
    public static final BedrockCodec DEFAULT_BEDROCK_CODEC = Bedrock_v630.CODEC;

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
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v589.CODEC.toBuilder()
            .minecraftVersion("1.20.0/1.20.1")
            .build());
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v594.CODEC.toBuilder()
            .minecraftVersion("1.20.10/1.20.15")
            .build());
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v618.CODEC.toBuilder()
            .minecraftVersion("1.20.30/1.20.32")
            .build());
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v622.CODEC.toBuilder()
            .minecraftVersion("1.20.40/1.20.41")
            .build());
        SUPPORTED_BEDROCK_CODECS.add(DEFAULT_BEDROCK_CODEC);
    }

    /**
     * Gets the {@link BedrockPacketCodec} of the given protocol version.
     * @param protocolVersion The protocol version to attempt to find
     * @return The packet codec, or null if the client's protocol is unsupported
     */
    public static BedrockCodec getBedrockCodec(int protocolVersion) {
        for (BedrockCodec packetCodec : SUPPORTED_BEDROCK_CODECS) {
            if (packetCodec.getProtocolVersion() == protocolVersion) {
                return packetCodec;
            }
        }
        return null;
    }

    /* Bedrock convenience methods to gatekeep features and easily remove the check on version removal */

    public static boolean isPre1_20_10(GeyserSession session) {
        return session.getUpstream().getProtocolVersion() < Bedrock_v594.CODEC.getProtocolVersion();
    }

    /**
     * @param session the session to check
     * @return true if the session needs an experiment for recipe unlocking
     */
    public static boolean isUsingExperimentalRecipeUnlocking(GeyserSession session) {
        return session.getUpstream().getProtocolVersion() == Bedrock_v594.CODEC.getProtocolVersion();
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
