/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.bedrock.nethernet;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannel;
import tel.schich.libdatachannel.DataChannel;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Reads how many bytes a NetherNet channel's data channels hold that native has yet to hand to SCTP.
 * TODO NetherNet: reflection until Cloudburst exposes its private queuedBytes()
 */
public final class NetherNetQueue {
    private static final @Nullable MethodHandle RELIABLE;
    private static final @Nullable MethodHandle UNRELIABLE;
    private static final @Nullable String FAILURE;

    static {
        MethodHandle reliable = null;
        MethodHandle unreliable = null;
        String failure = null;
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(NetherNetChannel.class, MethodHandles.lookup());
            reliable = lookup.findGetter(NetherNetChannel.class, "reliableChannel", DataChannel.class);
            unreliable = lookup.findGetter(NetherNetChannel.class, "unreliableChannel", DataChannel.class);
        } catch (ReflectiveOperationException | RuntimeException e) {
            failure = e.toString();
            reliable = null;
        }
        RELIABLE = reliable;
        UNRELIABLE = unreliable;
        FAILURE = failure;
    }

    private NetherNetQueue() {
    }

    /**
     * @return why queued bytes can't be read with this version of the NetherNet transport, or null if they can
     */
    public static @Nullable String unavailableReason() {
        return FAILURE;
    }

    /**
     * Safe to call from any thread.
     *
     * @return the bytes both data channels hold, or -1 if that can't be read
     */
    public static long queuedBytes(NetherNetChannel channel) {
        if (RELIABLE == null || UNRELIABLE == null) {
            return -1;
        }
        return buffered(RELIABLE, channel) + buffered(UNRELIABLE, channel);
    }

    private static long buffered(MethodHandle getter, NetherNetChannel channel) {
        try {
            DataChannel dataChannel = (DataChannel) getter.invokeExact(channel);
            return dataChannel == null ? 0 : dataChannel.bufferedAmount();
        } catch (RuntimeException closed) {
            return 0;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }
}
