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

package org.geysermc.geyser.network.codec;

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.v897.serializer.TextSerializer_v897;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;
import org.cloudburstmc.protocol.common.util.TextConverter;

// Sending empty messages kicks clients with 1.21.130...
public class CustomTextPacketSerializer extends TextSerializer_v897 {

    public static final CustomTextPacketSerializer INSTANCE = new CustomTextPacketSerializer();

    @Override
    public void serialize(ByteBuf buffer, BedrockCodecHelper helper, TextPacket packet) {
        TextPacket.Type type = packet.getType();
        TextConverter converter = helper.getTextConverter();
        CharSequence message = packet.getMessage(CharSequence.class);
        Boolean needsTranslation = converter.needsTranslation(message);

        buffer.writeBoolean(needsTranslation != null ? needsTranslation : packet.isNeedsTranslation());
        String msg;

        switch (type) {
            case RAW:
            case TIP:
            case SYSTEM:
                buffer.writeByte(0); // MessageOnly
                helper.writeString(buffer, "raw");
                helper.writeString(buffer, "tip");
                helper.writeString(buffer, "systemMessage");
                helper.writeString(buffer, "textObjectWhisper");
                helper.writeString(buffer, "textObjectAnnouncement");
                helper.writeString(buffer, "textObject");
                buffer.writeByte(type.ordinal());
                msg = converter.serialize(message);
                if (msg.isEmpty()) msg = " ";
                helper.writeString(buffer, msg);
                break;
            case JSON:
            case WHISPER_JSON:
            case ANNOUNCEMENT_JSON:
                buffer.writeByte(0); // MessageOnly
                helper.writeString(buffer, "raw");
                helper.writeString(buffer, "tip");
                helper.writeString(buffer, "systemMessage");
                helper.writeString(buffer, "textObjectWhisper");
                helper.writeString(buffer, "textObjectAnnouncement");
                helper.writeString(buffer, "textObject");
                buffer.writeByte(type.ordinal());
                msg = converter.serializeJson(message);
                if (msg.isEmpty()) msg = " ";
                helper.writeString(buffer, msg);
                break;
            case CHAT:
            case WHISPER:
            case ANNOUNCEMENT:
                buffer.writeByte(1); // AuthorAndMessage
                helper.writeString(buffer, "chat");
                helper.writeString(buffer, "whisper");
                helper.writeString(buffer, "announcement");
                buffer.writeByte(type.ordinal());
                helper.writeString(buffer, packet.getSourceName());
                msg = converter.serialize(message);
                if (msg.isEmpty()) msg = " ";
                helper.writeString(buffer, msg);
                break;
            case TRANSLATION:
            case POPUP:
            case JUKEBOX_POPUP:
                buffer.writeByte(2); // MessageAndParams
                helper.writeString(buffer, "translate");
                helper.writeString(buffer, "popup");
                helper.writeString(buffer, "jukeboxPopup");
                buffer.writeByte(type.ordinal());
                String text = converter.serializeWithArguments(message, packet.getParameters());
                if (text.isEmpty()) text = " ";
                helper.writeString(buffer, text);
                helper.writeArray(buffer, packet.getParameters(), helper::writeString);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported TextType " + type);
        }

        helper.writeString(buffer, packet.getXuid());
        helper.writeString(buffer, packet.getPlatformChatId());
        String filtered = converter.serialize(packet.getFilteredMessage(CharSequence.class));
        helper.writeOptional(buffer, (s -> !s.isEmpty()), filtered, (buf, codecHelper, s) -> codecHelper.writeString(buf, s));
    }
}
