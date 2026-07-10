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

import org.checkerframework.checker.index.qual.NonNegative;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.network.message.Message;

/**
 * A {@link PacketChannel} that operates on raw packet buffers rather than
 * a strongly typed packet class.
 * <p>
 * Use this when the message type does not correspond to an existing packet
 * implementation (i.e., a custom {@link Message.Packet}), in which case Geyser
 * cannot infer the packet ID from the class itself, and it must be supplied
 * explicitly.
 *
 * @since 2.9.2
 */
public interface RawPacketChannel extends PacketChannel {

    /**
     * Gets the packet ID associated with this channel.
     *
     * @return the packet ID
     */
    @NonNegative
    int packetId();

    /**
     * Creates a new raw Bedrock {@link PacketChannel} for the given packet ID
     * and message type.
     *
     * @param extension the extension creating the channel
     * @param packetId the packet ID
     * @param messageType the type of the message sent over this channel
     * @return a new raw Bedrock packet channel
     */
    static RawPacketChannel bedrock(Extension extension, @NonNegative int packetId, Class<? extends Message.Packet> messageType) {
        return GeyserApi.api().provider(RawPacketChannel.class, extension, "bedrock", packetId, messageType);
    }

    /**
     * Creates a new raw Java {@link PacketChannel} for the given packet ID
     * and message type.
     *
     * @param extension the extension creating the channel
     * @param packetId the packet ID
     * @param messageType the type of the message sent over this channel
     * @return a new raw Java packet channel
     */
    static RawPacketChannel java(Extension extension, @NonNegative int packetId, Class<? extends Message.Packet> messageType) {
        return GeyserApi.api().provider(RawPacketChannel.class, extension, "java", packetId, messageType);
    }
}
