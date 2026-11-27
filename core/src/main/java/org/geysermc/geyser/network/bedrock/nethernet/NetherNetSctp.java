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
import org.geysermc.geyser.GeyserImpl;
import tel.schich.libdatachannel.LibDataChannel;
import tel.schich.libdatachannel.SctpSettings;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lets testers turn off usrsctp's cwnd-based max burst with {@code -DGeyser.NetherNetCwndBasedMaxBurst=false}.
 * <p>
 * With it on (usrsctp's default), whenever the sender has less in flight than the congestion window allows, usrsctp
 * cuts the window to what is in flight plus max burst (10 MTUs). Geyser sends in bursts with quiet gaps between them,
 * such as chunks after crossing a chunk border, so each burst may start from about 12 KB of window and take several
 * round trips to go out. Whether turning it off helps is not known yet; this is here to find out.
 * <p>
 * The settings are global and apply to connections that are already open. Setting them replaces all of them, so this
 * repeats what the NetherNet transport sets itself (NetherNetNative), and runs once the transport has set those.
 */
public final class NetherNetSctp {
    private static final @Nullable Boolean CWND_BASED_MAX_BURST = parse(System.getProperty("Geyser.NetherNetCwndBasedMaxBurst"));
    /** What the NetherNet transport sets: Bedrock clients hold a SACK up to 200 ms, so a 200 ms minimum RTO fires early */
    private static final int MIN_RETRANSMIT_TIMEOUT_MS = 400;
    private static final AtomicBoolean APPLIED = new AtomicBoolean();

    private NetherNetSctp() {
    }

    /**
     * Applies the override once, if one is set. Called for each new NetherNet connection, by which time the transport
     * has initialized libdatachannel with its own settings.
     */
    static void applyOverride(GeyserImpl geyser) {
        if (CWND_BASED_MAX_BURST == null || !APPLIED.compareAndSet(false, true)) {
            return;
        }
        LibDataChannel.setSctpSettings(SctpSettings.builder()
            .minRetransmitTimeoutMs(MIN_RETRANSMIT_TIMEOUT_MS)
            .cwndBasedMaxBurst(CWND_BASED_MAX_BURST)
            .build());
        geyser.getLogger().info("[NetherNet] Testing usrsctp with " + describe());
    }

    /**
     * @return the cwnd-based max burst setting in use, for recordings
     */
    public static String describe() {
        return "cwnd-based max burst " + (CWND_BASED_MAX_BURST == null ? "on (default)" : CWND_BASED_MAX_BURST ? "on" : "off");
    }

    static @Nullable Boolean parse(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
