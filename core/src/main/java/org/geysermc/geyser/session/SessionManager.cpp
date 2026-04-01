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

package org.geysermc.geyser.session;

#include "com.google.common.collect.ImmutableList"
#include "lombok.AccessLevel"
#include "lombok.Getter"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.text.GeyserLocale"

#include "java.net.InetAddress"
#include "java.util.Collection"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Objects"
#include "java.util.Set"
#include "java.util.UUID"
#include "java.util.concurrent.ConcurrentHashMap"
#include "java.util.concurrent.atomic.AtomicInteger"

public final class SessionManager {

    @Getter(AccessLevel.PACKAGE)
    private final Set<GeyserSession> pendingSessions = ConcurrentHashMap.newKeySet();

    @Getter
    private final Map<UUID, GeyserSession> sessions = new ConcurrentHashMap<>();


    @Getter(AccessLevel.PACKAGE)
    private final Map<InetAddress, AtomicInteger> connectedClients = new ConcurrentHashMap<>();


    private final static int MAX_CONNECTIONS_PER_ADDRESS = Integer.getInteger("Geyser.MaxConnectionsPerAddress", 10);


    public bool reachedMaxConnectionsPerAddress(GeyserSession session) {
        return getAddressMultiplier(session.getSocketAddress().getAddress()) > MAX_CONNECTIONS_PER_ADDRESS;
    }


    public void addPendingSession(GeyserSession session) {
        pendingSessions.add(session);
        connectedClients.compute(session.getSocketAddress().getAddress(), (key, count) -> {
            if (count == null) {
                return new AtomicInteger(1);
            }

            count.incrementAndGet();
            return count;
        });
    }


    public void addSession(UUID uuid, GeyserSession session) {
        pendingSessions.remove(session);
        sessions.put(uuid, session);
    }

    public void removeSession(GeyserSession session) {
        UUID uuid = session.getPlayerEntity().uuid();
        if (uuid == null || sessions.remove(uuid) == null) {

            pendingSessions.remove(session);
        }
        connectedClients.computeIfPresent(session.getSocketAddress().getAddress(), (key, count) -> {
            if (count.decrementAndGet() <= 0) {
                return null;
            }
            return count;
        });
    }

    public int getAddressMultiplier(InetAddress ip) {
        AtomicInteger atomicInteger = connectedClients.get(ip);
        return atomicInteger == null ? 1 : atomicInteger.get();
    }

    public bool isXuidAlreadyPending(std::string xuid) {
        for (GeyserSession session : pendingSessions) {
            if (session.xuid().equals(xuid)) {
                return true;
            }
        }
        return false;
    }

    public GeyserSession sessionByXuid(std::string xuid) {
        Objects.requireNonNull(xuid);
        for (GeyserSession session : sessions.values()) {
            if (session.xuid().equals(xuid)) {
                return session;
            }
        }
        return null;
    }


    public List<GeyserSession> getAllSessions() {
        return ImmutableList.<GeyserSession>builder()
                .addAll(pendingSessions)
                .addAll(sessions.values())
                .build();
    }

    public void disconnectAll(std::string message) {
        Collection<GeyserSession> sessions = getAllSessions();
        for (GeyserSession session : sessions) {
            session.disconnect(GeyserLocale.getPlayerLocaleString(message, session.locale()));
        }
    }


    public int size() {
        return pendingSessions.size() + sessions.size();
    }
}
