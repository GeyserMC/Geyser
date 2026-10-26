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

package org.geysermc.geyser.session;

import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityDeltaPacket;
import org.geysermc.geyser.network.bedrock.GeyserBedrockPeer;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpstreamSessionTest {
    private final BedrockPacket move = new MoveEntityDeltaPacket();

    private static UpstreamSession sessionInSlot(GeyserBedrockPeer peer, int slot) {
        BedrockServerSession bedrockSession = mock(BedrockServerSession.class);
        when(bedrockSession.getPeer()).thenReturn(peer);
        when(bedrockSession.isSubClient()).thenReturn(slot != 0);
        when(peer.isConnected()).thenReturn(true);
        when(peer.subClientIdOf(bedrockSession)).thenReturn(slot);
        return new UpstreamSession(bedrockSession);
    }

    @Test
    void primaryFlushesToSubClientZero() {
        GeyserBedrockPeer peer = mock(GeyserBedrockPeer.class);
        sessionInSlot(peer, 0).sendPacketsImmediately(move);
        verify(peer).sendPacketsImmediately(0, 0, move);
    }

    @Test
    void guestFlushesToItsOwnSubClient() {
        GeyserBedrockPeer peer = mock(GeyserBedrockPeer.class);
        sessionInSlot(peer, 1).sendPacketsImmediately(move);
        verify(peer).sendPacketsImmediately(1, 0, move);
        verify(peer, never()).sendPacketsImmediately(0, 0, move);
    }

    @Test
    void evictedGuestSendsNothing() {
        GeyserBedrockPeer peer = mock(GeyserBedrockPeer.class);
        sessionInSlot(peer, -1).sendPacketsImmediately(move);
        verify(peer, never()).sendPacketsImmediately(anyInt(), anyInt(), any(BedrockPacket[].class));
    }
}
