/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketSerializer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.CodeBuilderSourcePacket;
import org.cloudburstmc.protocol.bedrock.packet.CreatePhotoPacket;
import org.cloudburstmc.protocol.bedrock.packet.GameTestRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.LabTablePacket;
import org.cloudburstmc.protocol.bedrock.packet.NpcRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.PhotoInfoRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.PhotoTransferPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;

/**
 * Produces an education-aware codec from the base processed codec.
 * Re-enables education-specific packets (chemistry, NPCs, photos, Code Builder)
 * that are marked ILLEGAL in the base codec, using a no-op serializer
 * rather than disconnecting the client.
 */
public final class EducationCodecProcessor {

    /**
     * No-op serializer that silently ignores packets during deserialization.
     * Used for education packets that Geyser has no translators for.
     */
    @SuppressWarnings("rawtypes")
    private static final BedrockPacketSerializer IGNORED_SERIALIZER = new BedrockPacketSerializer<>() {
        @Override
        public void serialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
        }

        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
        }
    };

    private EducationCodecProcessor() {
    }

    /**
     * Wraps the given base codec with education-specific overrides.
     * The base codec has education packets marked as ILLEGAL (disconnects client).
     * This method overrides them to IGNORED (no-op deserialization) so education
     * clients can send these packets without being disconnected.
     *
     * @param codec the base processed codec (from {@link CodecProcessor#processCodec})
     * @return a new codec with education packets re-enabled
     */
    @SuppressWarnings("unchecked")
    public static BedrockCodec educationCodec(BedrockCodec codec) {
        return codec.toBuilder()
            .updateSerializer(StartGamePacket.class, EducationStartGameSerializer.INSTANCE)
            // Re-enable education packets that are ILLEGAL in the base codec.
            // Education clients legitimately send these for chemistry, NPCs, photos, and Code Builder.
            // IGNORED allows deserialization without processing (Geyser has no translators for these).
            .updateSerializer(PhotoTransferPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(LabTablePacket.class, IGNORED_SERIALIZER)
            .updateSerializer(CodeBuilderSourcePacket.class, IGNORED_SERIALIZER)
            .updateSerializer(CreatePhotoPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(NpcRequestPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(PhotoInfoRequestPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(GameTestRequestPacket.class, IGNORED_SERIALIZER)
            .build();
    }
}
