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

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.geysermc.geyser.api.event.lifecycle.GeyserBedrockPingEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.Objects;

public class GeyserBedrockPingEventImpl implements GeyserBedrockPingEvent {
    private final InetSocketAddress address;
    private final BedrockPong pong;

    public GeyserBedrockPingEventImpl(BedrockPong pong, InetSocketAddress address) {
        this.address = address;
        this.pong = pong;
    }

    @Override
    public void setPrimaryMotd(@Nonnull String primary) {
        pong.motd(Objects.requireNonNull(primary, "Primary MOTD cannot be null"));
    }

    @Override
    public void setSecondaryMotd(@Nonnull String secondary) {
        pong.subMotd(Objects.requireNonNull(secondary, "Secondary MOTD cannot be null"));
    }

    @Override
    public void setPlayerCount(int count) {
        if (count < 0) throw new IllegalArgumentException("Player count cannot be below 0");
        pong.playerCount(count);
    }

    @Override
    public void setMaxPlayerCount(int max) {
        if (max < 1) throw new IllegalArgumentException("Max player count cannot be below 1");
        pong.maximumPlayerCount(max);
    }

    @Override
    public @Nullable String getPrimaryMotd() {
        return pong.motd();
    }

    @Override
    public @Nullable String getSecondaryMotd() {
        return pong.subMotd();
    }

    @Override
    public @NonNegative int getPlayerCount() {
        return pong.playerCount();
    }

    @Override
    public int getMaxPlayerCount() {
        return pong.maximumPlayerCount();
    }

    @Override
    public @NotNull InetSocketAddress getAddress() {
        return address;
    }
}
