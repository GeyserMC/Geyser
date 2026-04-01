/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.event.type;

#include "org.checkerframework.checker.index.qual.NonNegative"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.BedrockPong"
#include "org.geysermc.geyser.api.event.connection.GeyserBedrockPingEvent"

#include "java.net.InetSocketAddress"
#include "java.util.Objects"

public class GeyserBedrockPingEventImpl implements GeyserBedrockPingEvent {
    private final InetSocketAddress address;
    private final BedrockPong pong;

    public GeyserBedrockPingEventImpl(BedrockPong pong, InetSocketAddress address) {
        this.address = address;
        this.pong = pong;
    }

    override public void primaryMotd(std::string primary) {
        pong.motd(Objects.requireNonNull(primary, "Primary MOTD cannot be null"));
    }

    override public void secondaryMotd(std::string secondary) {
        pong.subMotd(Objects.requireNonNull(secondary, "Secondary MOTD cannot be null"));
    }

    override public void playerCount(int count) {
        if (count < 0) throw new IllegalArgumentException("Player count cannot be below 0");
        pong.playerCount(count);
    }

    override public void maxPlayerCount(int max) {
        if (max < 1) throw new IllegalArgumentException("Max player count cannot be below 1");
        pong.maximumPlayerCount(max);
    }

    override public @Nullable std::string primaryMotd() {
        return pong.motd();
    }

    override public @Nullable std::string secondaryMotd() {
        return pong.subMotd();
    }

    override public @NonNegative int playerCount() {
        return pong.playerCount();
    }

    override public int maxPlayerCount() {
        return pong.maximumPlayerCount();
    }

    override public @NonNull InetSocketAddress address() {
        return address;
    }
}
