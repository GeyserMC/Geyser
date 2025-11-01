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

package org.geysermc.geyser.network.message;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketDefinition;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketSerializer;
import org.cloudburstmc.protocol.bedrock.codec.PacketSerializeException;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.geyser.api.network.message.Message;
import org.geysermc.geyser.session.GeyserSession;

public record BedrockPacketMessage(@NonNull BedrockPacket packet) implements Message.PacketWrapped<ByteBufMessageBuffer> {

    @SuppressWarnings("unchecked")
    public void postProcess(@NonNull GeyserSession session, @NonNull ByteBufMessageBuffer buffer) {
        BedrockPacketDefinition<? extends BedrockPacket> definition = session.getUpstream().getSession().getCodec().getPacketDefinition(this.packet.getClass());
        if (definition == null) {
            throw new IllegalArgumentException("Packet definition for " + this.packet.getClass().getSimpleName() + " not found!");
        }

        BedrockPacketSerializer<BedrockPacket> serializer = (BedrockPacketSerializer<BedrockPacket>) definition.getSerializer();
        try {
            serializer.deserialize(buffer.buffer(), session.getUpstream().getCodecHelper(), this.packet);
        } catch (Exception e) {
            throw new PacketSerializeException("Error whilst deserializing " + this.packet, e);
        }
    }

    @Override
    public void encode(@NonNull ByteBufMessageBuffer buffer) {
        throw new UnsupportedOperationException("BedrockPacketMessage does not support encoding directly to a MessageBuffer.");
    }
}
