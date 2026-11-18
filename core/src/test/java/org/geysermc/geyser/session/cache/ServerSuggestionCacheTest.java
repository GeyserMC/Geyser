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

import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServerSuggestionCacheTest {
    private GeyserSession session;
    private ServerSuggestionCache cache;
    private int resends;

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

        SessionPlayerEntity self = mock(SessionPlayerEntity.class);
        when(self.getUsername()).thenReturn("Steve");
        when(session.getPlayerEntity()).thenReturn(self);
        EntityCache entityCache = mock(EntityCache.class);
        when(entityCache.getPlayersByName(anyString())).thenReturn(List.of());
        when(entityCache.getPlayersByName("Alex")).thenReturn(List.of(mock(PlayerEntity.class)));
        when(session.getEntityCache()).thenReturn(entityCache);

        cache = new ServerSuggestionCache(session);
    }

    private List<String> sentTexts() {
        ArgumentCaptor<ServerboundCommandSuggestionPacket> captor = ArgumentCaptor.forClass(ServerboundCommandSuggestionPacket.class);
        verify(session, atLeastOnce()).sendDownstreamGamePacket(captor.capture());
        return captor.getAllValues().stream().map(ServerboundCommandSuggestionPacket::getText).toList();
    }

    private int transactionOf(String text) {
        ArgumentCaptor<ServerboundCommandSuggestionPacket> captor = ArgumentCaptor.forClass(ServerboundCommandSuggestionPacket.class);
        verify(session, atLeastOnce()).sendDownstreamGamePacket(captor.capture());
        return captor.getAllValues().stream().filter(p -> p.getText().equals(text)).reduce((a, b) -> b).orElseThrow().getTransactionId();
    }

    @Test
    void asksTheServerForEveryArgument() {
        cache.setRequests(Set.of("/home ", "/warp "), () -> resends++);

        assertEquals(List.of("/home ", "/warp "), sentTexts().stream().sorted().toList());
    }

    @Test
    void answersSendTheCommandsAgain() {
        cache.setRequests(Set.of("/home "), () -> resends++);

        assertTrue(cache.handleResponse(transactionOf("/home "), new String[] {"base", "farm", "two words"}));

        assertEquals(1, resends);
        assertEquals(Set.of("base", "farm"), cache.suggestions("/home "));
    }

    @Test
    void asksWhatFollowsEachValue() {
        cache.setRequests(Set.of("/clan "), () -> resends++);
        cache.handleResponse(transactionOf("/clan "), new String[] {"invite", "leave"});

        assertTrue(sentTexts().containsAll(List.of("/clan invite ", "/clan leave ")));
        cache.handleResponse(transactionOf("/clan invite "), new String[] {"Alex"});
        assertEquals(Set.of("Alex"), cache.suggestionsAfter("/clan ", "invite"));
    }

    @Test
    void aliasesShareTheSecondLevel() {
        cache.setRequests(Set.of("/clan ", "/guild "), () -> resends++);
        cache.handleResponse(transactionOf("/clan "), new String[] {"invite"});
        cache.handleResponse(transactionOf("/guild "), new String[] {"invite"});
        cache.handleResponse(transactionOf("/clan invite "), new String[] {"Alex"});

        assertFalse(sentTexts().contains("/guild invite "));
        assertEquals(Set.of("Alex"), cache.suggestionsAfter("/guild ", "invite"));
    }

    @Test
    void playerListsAndLongListsHaveNoSecondLevel() {
        cache.setRequests(Set.of("/tell ", "/help "), () -> resends++);
        cache.handleResponse(transactionOf("/tell "), new String[] {"Alex", "Steve"});
        String[] commands = new String[40];
        for (int i = 0; i < commands.length; i++) {
            commands[i] = "command" + i;
        }
        cache.handleResponse(transactionOf("/help "), commands);

        assertEquals(2, sentTexts().size());
        assertTrue(cache.onlyPlayers(cache.suggestions("/tell ")));
        assertFalse(cache.onlyPlayers(Set.of("Alex", "base")));
    }

    @Test
    void everyKnownPlayerIsStillAPlayerList() {
        // offline names too, but the list has the player's own name
        assertTrue(cache.onlyPlayers(Set.of("Steve", "Alex", "Notch")));
        assertFalse(cache.onlyPlayers(Set.of("invite", "leave", "kick")));
    }

    @Test
    void unchangedAnswersAreNotSentAgain() {
        cache.setRequests(Set.of("/home "), () -> resends++);
        cache.handleResponse(transactionOf("/home "), new String[] {"base"});
        cache.handleResponse(transactionOf("/home base "), new String[0]);

        cache.refreshSoon("home base");
        cache.handleResponse(transactionOf("/home "), new String[] {"base"});

        assertEquals(1, resends);
    }

    @Test
    void sameCommandTreeIsNotRequestedAgain() {
        cache.setRequests(Set.of("/home "), () -> resends++);
        cache.setRequests(Set.of("/home "), () -> resends++);

        assertEquals(1, sentTexts().size());
    }

    @Test
    void runningACommandOnlyRefreshesThatCommand() {
        cache.setRequests(Set.of("/home ", "/warp "), () -> resends++);
        cache.refreshSoon("HOME base");

        List<String> texts = sentTexts();
        assertEquals(3, texts.size());
        assertEquals("/home ", texts.get(2));
    }

    @Test
    void ignoresAnswersItDidNotAskFor() {
        cache.setRequests(Set.of("/home "), () -> resends++);

        assertFalse(cache.handleResponse(12345, new String[] {"base"}));
        assertEquals(0, resends);
    }
}
