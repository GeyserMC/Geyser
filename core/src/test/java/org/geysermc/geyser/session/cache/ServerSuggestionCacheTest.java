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

package org.geysermc.geyser.session.cache;

import org.cloudburstmc.protocol.bedrock.data.command.SoftEnumUpdateType;
import org.cloudburstmc.protocol.bedrock.packet.UpdateSoftEnumPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ServerSuggestionCacheTest {
    private GeyserSession session;
    private ServerSuggestionCache cache;

    @BeforeEach
    void setUp() {
        session = mock(GeyserSession.class);
        // Run everything right away: the event loop is not needed to check what is sent
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(session).ensureInEventLoop(any());
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(session).scheduleInEventLoop(any(), anyLong(), any());
        cache = new ServerSuggestionCache(session);
    }

    private List<ServerboundCommandSuggestionPacket> sentRequests(int count) {
        ArgumentCaptor<ServerboundCommandSuggestionPacket> captor = ArgumentCaptor.forClass(ServerboundCommandSuggestionPacket.class);
        verify(session, times(count)).sendDownstreamGamePacket(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void asksTheServerForEveryArgument() {
        cache.setRequests(Map.of("home_args", "/home ", "warp_args", "/warp "));

        List<String> texts = sentRequests(2).stream().map(ServerboundCommandSuggestionPacket::getText).sorted().toList();
        assertEquals(List.of("/home ", "/warp "), texts);
    }

    @Test
    void answersFillTheSoftEnum() {
        cache.setRequests(Map.of("home_args", "/home "));
        int transactionId = sentRequests(1).get(0).getTransactionId();

        assertTrue(cache.handleResponse(transactionId, new String[] {"base", "farm", "two words"}));

        ArgumentCaptor<UpdateSoftEnumPacket> captor = ArgumentCaptor.forClass(UpdateSoftEnumPacket.class);
        verify(session).sendUpstreamPacket(captor.capture());
        UpdateSoftEnumPacket packet = captor.getValue();
        assertEquals(SoftEnumUpdateType.REPLACE, packet.getType());
        assertEquals("home_args", packet.getSoftEnum().getName());
        assertTrue(packet.getSoftEnum().isSoft());
        assertEquals(Set.of("base", "farm"), packet.getSoftEnum().getValues().keySet());
        assertEquals(Set.of("base", "farm"), cache.knownValues("home_args").keySet());
    }

    @Test
    void unchangedAnswersAreNotSentAgain() {
        cache.setRequests(Map.of("home_args", "/home "));
        cache.handleResponse(sentRequests(1).get(0).getTransactionId(), new String[] {"base"});

        cache.refreshSoon();
        cache.handleResponse(sentRequests(2).get(1).getTransactionId(), new String[] {"base"});

        verify(session, times(1)).sendUpstreamPacket(any());
    }

    @Test
    void ignoresAnswersItDidNotAskFor() {
        cache.setRequests(Map.of("home_args", "/home "));

        assertFalse(cache.handleResponse(12345, new String[] {"base"}));
        verify(session, never()).sendUpstreamPacket(any());
    }
}
