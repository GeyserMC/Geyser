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
import org.cloudburstmc.protocol.bedrock.codec.v898.Bedrock_v898;
import org.cloudburstmc.protocol.bedrock.codec.v924.Bedrock_v924;
import org.cloudburstmc.protocol.bedrock.codec.v944.Bedrock_v944;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.geysermc.geyser.api.util.MinecraftVersion;
import org.geysermc.geyser.impl.MinecraftVersionImpl;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.codec.PacketCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public final class GameProtocol {

    
    private static final List<BedrockCodec> SUPPORTED_BEDROCK_CODECS = new ArrayList<>();

    
    public static final IntList SUPPORTED_BEDROCK_PROTOCOLS = new IntArrayList();

    
    public static final List<MinecraftVersion> SUPPORTED_BEDROCK_VERSIONS = new ArrayList<>();

    
    public static final int DEFAULT_BEDROCK_PROTOCOL;

    
    public static final String DEFAULT_BEDROCK_VERSION;

    
    private static final PacketCodec DEFAULT_JAVA_CODEC = MinecraftCodec.CODEC;

    static {
        
        register(Bedrock_v898.CODEC, "1.21.130", "1.21.131", "1.21.132");
        register(Bedrock_v924.CODEC, "26.0", "26.1", "26.2", "26.3");
        register(Bedrock_v944.CODEC, "26.10");

        MinecraftVersion latestBedrock = SUPPORTED_BEDROCK_VERSIONS.get(SUPPORTED_BEDROCK_VERSIONS.size() - 1);
        DEFAULT_BEDROCK_VERSION = latestBedrock.versionString();
        DEFAULT_BEDROCK_PROTOCOL = latestBedrock.protocolVersion();
    }

    
    private static void register(BedrockCodec codec, String... minecraftVersions) {
        
        codec = CodecProcessor.processCodec(codec);

        SUPPORTED_BEDROCK_CODECS.add(codec);
        SUPPORTED_BEDROCK_PROTOCOLS.add(codec.getProtocolVersion());

        for (String version : minecraftVersions) {
            SUPPORTED_BEDROCK_VERSIONS.add(new MinecraftVersionImpl(version, codec.getProtocolVersion()));
        }
    }

    
    private static void register(BedrockCodec codec) {
        register(codec, codec.getMinecraftVersion());
    }

    
    public static @Nullable BedrockCodec getBedrockCodec(int protocolVersion) {
        for (BedrockCodec packetCodec : SUPPORTED_BEDROCK_CODECS) {
            if (packetCodec.getProtocolVersion() == protocolVersion) {
                return packetCodec;
            }
        }
        return null;
    }

    /* Bedrock convenience methods to gatekeep features and easily remove the check on version removal */

    public static boolean is1_26_10orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v944.CODEC.getProtocolVersion();
    }

    
    public static List<String> getJavaVersions() {
        return List.of(DEFAULT_JAVA_CODEC.getMinecraftVersion());
    }

    
    public static int getJavaProtocolVersion() {
        return DEFAULT_JAVA_CODEC.getProtocolVersion();
    }

    
    public static String getJavaMinecraftVersion() {
        return DEFAULT_JAVA_CODEC.getMinecraftVersion();
    }

    
    public static String getAllSupportedBedrockVersions() {
        return SUPPORTED_BEDROCK_VERSIONS.stream()
            .map(MinecraftVersion::versionString)
            .collect(Collectors.joining(", "));
    }

    
    public static String getAllSupportedJavaVersions() {
        return String.join(", ", getJavaVersions());
    }

    private GameProtocol() {
    }
}
