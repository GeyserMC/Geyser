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

package org.geysermc.connector.network;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.v419.Bedrock_v419;
import com.nukkitx.protocol.bedrock.v422.Bedrock_v422;
import com.nukkitx.protocol.bedrock.v428.Bedrock_v428;
import com.nukkitx.protocol.bedrock.v431.Bedrock_v431;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains information about the supported Bedrock protocols in Geyser.
 */
public class BedrockProtocol {
    /**
     * Default Bedrock codec that should act as a fallback. Should represent the latest available
     * release of the game that Geyser supports.
     */
    public static final BedrockPacketCodec DEFAULT_BEDROCK_CODEC = Bedrock_v431.V431_CODEC;
    /**
     * A list of all supported Bedrock versions that can join Geyser
     */
    public static final List<BedrockPacketCodec> SUPPORTED_BEDROCK_CODECS = new ArrayList<>();

    static {
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v419.V419_CODEC.toBuilder()
                .minecraftVersion("1.16.100/1.16.101") // We change this as 1.16.100.60 is a beta
                .build());
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v422.V422_CODEC.toBuilder()
                .minecraftVersion("1.16.200/1.16.201")
                .build());
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v428.V428_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(DEFAULT_BEDROCK_CODEC);
    }

    /**
     * Gets the {@link BedrockPacketCodec} of the given protocol version.
     * @param protocolVersion The protocol version to attempt to find
     * @return The packet codec, or null if the client's protocol is unsupported
     */
    public static BedrockPacketCodec getBedrockCodec(int protocolVersion) {
        for (BedrockPacketCodec packetCodec : SUPPORTED_BEDROCK_CODECS) {
            if (packetCodec.getProtocolVersion() == protocolVersion) {
                return packetCodec;
            }
        }
        return null;
    }
}
