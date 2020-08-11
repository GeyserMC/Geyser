/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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
import com.nukkitx.protocol.bedrock.v407.Bedrock_v407;
import com.nukkitx.protocol.bedrock.v408.Bedrock_v408;
import com.nukkitx.protocol.bedrock.v409.Bedrock_v409;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains information about the supported Bedrock protocols in Geyser.
 */
public class BedrockProtocol {
    /**
     * Default Bedrock codec that should act as a fallback and as the version shown in /geyser version
     */
    public static final BedrockPacketCodec DEFAULT_BEDROCK_CODEC = Bedrock_v408.V408_CODEC;
    /**
     * A list of all supported Bedrock versions that can join Geyser
     */
    public static final Set<BedrockPacketCodec> SUPPORTED_BEDROCK_CODECS = ConcurrentHashMap.newKeySet();

    static {
        SUPPORTED_BEDROCK_CODECS.add(DEFAULT_BEDROCK_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v407.V407_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v409.V409_CODEC);
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
