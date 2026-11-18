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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.cloudburstmc.protocol.bedrock.data.command.SoftEnumUpdateType;
import org.cloudburstmc.protocol.bedrock.packet.UpdateSoftEnumPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Bedrock clients never ask the server for command suggestions, so arguments whose suggestions
 * are computed by the Java server (the "minecraft:ask_server" suggestion type, used by most plugin commands)
 * would show no options. Geyser asks the Java server on behalf of the player and keeps the answers
 * in soft enums, which Bedrock can update at any time.
 */
public final class ServerSuggestionCache {
    /**
     * Time between two suggestion requests. Servers rate limit them (Paper drops more than 15 per second by default).
     */
    private static final long REQUEST_INTERVAL_MILLIS = 100;
    /**
     * Commands often change what other commands suggest (setting a home, joining a team...), so the
     * suggestions are requested again shortly after the player runs a command.
     */
    private static final long REFRESH_DELAY_MILLIS = 2000;
    private static final int MAX_VALUES = 200;

    private final GeyserSession session;
    /**
     * Soft enum name to the command text sent to the Java server, e.g. "/home ".
     */
    private Map<String, String> requests = Map.of();
    private final Map<String, Set<String>> values = new HashMap<>();
    private final Int2ObjectMap<String> pending = new Int2ObjectOpenHashMap<>();
    private final Deque<String> queue = new ArrayDeque<>();
    private int nextTransactionId = 1;
    private boolean draining;
    private boolean refreshScheduled;

    public ServerSuggestionCache(GeyserSession session) {
        this.session = session;
    }

    /**
     * @return the last suggestions received for this soft enum, to keep them when the commands are sent again
     */
    public Map<String, Set<CommandEnumConstraint>> knownValues(String enumName) {
        Map<String, Set<CommandEnumConstraint>> known = new LinkedHashMap<>();
        for (String value : values.getOrDefault(enumName, Set.of())) {
            known.put(value, Set.of());
        }
        return known;
    }

    /**
     * Replaces the arguments to fill with server suggestions and requests all of them.
     */
    public void setRequests(Map<String, String> requests) {
        session.ensureInEventLoop(() -> {
            this.requests = Map.copyOf(requests);
            this.values.keySet().retainAll(this.requests.keySet());
            this.pending.clear();
            this.queue.clear();
            requestAll();
        });
    }

    /**
     * Requests every suggestion again in a moment, once for a burst of commands.
     */
    public void refreshSoon() {
        session.ensureInEventLoop(() -> {
            if (refreshScheduled || requests.isEmpty()) {
                return;
            }
            refreshScheduled = true;
            session.scheduleInEventLoop(() -> {
                refreshScheduled = false;
                requestAll();
            }, REFRESH_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        });
    }

    private void requestAll() {
        for (String enumName : requests.keySet()) {
            if (!queue.contains(enumName)) {
                queue.add(enumName);
            }
        }
        if (!draining) {
            drain();
        }
    }

    private void drain() {
        String enumName = queue.poll();
        if (enumName == null) {
            draining = false;
            return;
        }
        draining = true;
        String text = requests.get(enumName);
        if (text != null) {
            int transactionId = nextTransactionId++;
            pending.put(transactionId, enumName);
            session.sendDownstreamGamePacket(new ServerboundCommandSuggestionPacket(transactionId, text));
        }
        session.scheduleInEventLoop(this::drain, REQUEST_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * @return false if the response was not requested by this cache (a Java client behind Geyser never asks)
     */
    public boolean handleResponse(int transactionId, String[] matches) {
        String enumName = pending.remove(transactionId);
        if (enumName == null) {
            return false;
        }
        if (!requests.containsKey(enumName)) {
            return true;
        }

        Set<String> newValues = new LinkedHashSet<>();
        for (String match : matches) {
            // Enum values are single words; the server could still suggest a whole phrase
            if (!match.isBlank() && match.chars().noneMatch(Character::isWhitespace)) {
                newValues.add(match);
            }
            if (newValues.size() >= MAX_VALUES) {
                break;
            }
        }
        if (newValues.equals(values.getOrDefault(enumName, Set.of()))) {
            return true;
        }
        values.put(enumName, newValues);

        Map<String, Set<CommandEnumConstraint>> enumValues = new LinkedHashMap<>();
        for (String value : newValues) {
            enumValues.put(value, Collections.emptySet());
        }
        UpdateSoftEnumPacket packet = new UpdateSoftEnumPacket();
        packet.setType(SoftEnumUpdateType.REPLACE);
        packet.setSoftEnum(new CommandEnumData(enumName, enumValues, true));
        session.sendUpstreamPacket(packet);
        return true;
    }
}
