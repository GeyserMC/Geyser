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

package org.geysermc.geyser.network;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.network.PacketChannel;
import org.geysermc.geyser.api.util.Identifier;

import java.util.Objects;

public class PacketChannelImpl extends ExternalNetworkChannel implements PacketChannel {
    private final int packetId;
    private final boolean java;

    public PacketChannelImpl(@NonNull Identifier identifier, boolean java, @NonNegative int packetId, @NonNull Class<?> packetType) {
        super(identifier, packetType);

        this.java = java;
        this.packetId = packetId;
    }

    @NonNegative
    public int packetId() {
        return this.packetId;
    }

    public boolean isJava() {
        return this.java;
    }

    @Override
    public boolean isPacket() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PacketChannelImpl that = (PacketChannelImpl) o;
        return this.packetId == that.packetId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.packetId);
    }
}
