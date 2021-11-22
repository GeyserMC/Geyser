/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Getter;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {
    /**
     * A list of all players who don't currently have a permanent UUID attached yet.
     */
    @Getter(AccessLevel.PACKAGE)
    private final Set<GeyserSession> pendingSessions = ConcurrentHashMap.newKeySet();
    /**
     * A list of all players who are currently in-game.
     */
    @Getter
    private final Map<UUID, GeyserSession> sessions = new ConcurrentHashMap<>();

    /**
     * Called once the player has successfully authenticated to the Geyser server.
     */
    public void addPendingSession(GeyserSession session) {
        pendingSessions.add(session);
    }

    /**
     * Called once a player has successfully logged into their Java server.
     */
    public void addSession(UUID uuid, GeyserSession session) {
        pendingSessions.remove(session);
        sessions.put(uuid, session);
    }

    public void removeSession(GeyserSession session) {
        if (sessions.remove(session.getPlayerEntity().getUuid()) == null) {
            // Connection was likely pending
            pendingSessions.remove(session);
        }
    }

    /**
     * Creates a new, immutable list containing all pending and active sessions.
     */
    public List<GeyserSession> getAllSessions() {
        return ImmutableList.<GeyserSession>builder() // builderWithExpectedSize is probably not a good idea yet as older Spigot builds probably won't have it.
                .addAll(pendingSessions)
                .addAll(sessions.values())
                .build();
    }

    public void disconnectAll(String message) {
        Collection<GeyserSession> sessions = getAllSessions();
        for (GeyserSession session : sessions) {
            session.disconnect(GeyserLocale.getPlayerLocaleString(message, session.getLocale()));
        }
    }

    /**
     * @return the total amount of sessions, including those pending.
     */
    public int size() {
        return pendingSessions.size() + sessions.size();
    }
}
