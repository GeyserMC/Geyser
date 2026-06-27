/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.network;

import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.network.message.Message;

/**
 * Represents a channel for network communication associated with a packet.
 * <p>
 * This channel is used for listening to communication over
 * packets between the server and client and can be used to
 * send or receive packets.
 * <p>
 * When the message type is the actual packet class (i.e., a Cloudburst Bedrock
 * packet or an MCProtocolLib Java packet), Geyser can derive the packet ID
 * from the class itself, so no ID is required here. For channels that operate
 * on raw buffers via {@link Message.Packet}
 * implementations, use {@link RawPacketChannel} instead, which requires the
 * packet ID to be explicitly set.
 *
 * @since 2.9.2
 */
public interface PacketChannel extends NetworkChannel {

    /**
     * Creates a new Bedrock {@link PacketChannel} keyed by the given packet class.
     *
     * @param extension the extension creating the channel
     * @param packetType the packet class this channel handles
     * @return a new Bedrock packet channel
     */
    static PacketChannel bedrock(Extension extension, Class<?> packetType) {
        return GeyserApi.api().provider(PacketChannel.class, extension, "bedrock", packetType);
    }

    /**
     * Creates a new Java {@link PacketChannel} keyed by the given packet class.
     *
     * @param extension the extension creating the channel
     * @param packetType the packet class this channel handles
     * @return a new Java packet channel
     */
    static PacketChannel java(Extension extension, Class<?> packetType) {
        return GeyserApi.api().provider(PacketChannel.class, extension, "java", packetType);
    }
}
