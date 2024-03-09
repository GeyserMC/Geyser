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

package org.geysermc.geyser.text;

import com.github.steveice10.mc.protocol.data.game.chat.BuiltinChatType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public record ChatTypeEntry(TextPacket.@NonNull Type bedrockChatType, @Nullable TextDecoration textDecoration) {
    private static final ChatTypeEntry CHAT = new ChatTypeEntry(TextPacket.Type.CHAT, null);
    private static final ChatTypeEntry RAW = new ChatTypeEntry(TextPacket.Type.RAW, null);

    /**
     * Apply defaults to a map so it isn't empty in the event a chat message is sent before the login packet.
     */
    public static void applyDefaults(Int2ObjectMap<ChatTypeEntry> chatTypes) {
        // So the proper way to do this, probably, would be to dump the NBT data from vanilla and load it.
        // But, the only way this happens is if a chat message is sent to us before the login packet, which is rare.
        // So we'll just make sure chat ends up in the right place.
        chatTypes.put(BuiltinChatType.CHAT.ordinal(), CHAT);
        chatTypes.put(BuiltinChatType.SAY_COMMAND.ordinal(), RAW);
        chatTypes.put(BuiltinChatType.MSG_COMMAND_INCOMING.ordinal(), RAW);
        chatTypes.put(BuiltinChatType.MSG_COMMAND_OUTGOING.ordinal(), RAW);
        chatTypes.put(BuiltinChatType.TEAM_MSG_COMMAND_INCOMING.ordinal(), RAW);
        chatTypes.put(BuiltinChatType.TEAM_MSG_COMMAND_OUTGOING.ordinal(), RAW);
        chatTypes.put(BuiltinChatType.EMOTE_COMMAND.ordinal(), RAW);
    }
}
