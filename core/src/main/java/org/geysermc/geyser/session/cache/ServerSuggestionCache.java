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
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Bedrock clients never ask the server for command suggestions, so arguments whose suggestions
 * are computed by the Java server (the "minecraft:ask_server" suggestion type, used by most plugin commands)
 * would show no options. Geyser asks the Java server on behalf of the player and keeps the answers
 * in soft enums.
 * The client does not show values that arrive later through UpdateSoftEnumPacket, so the commands are sent again
 * once the answers are in.
 */
public final class ServerSuggestionCache {
    /**
     * Time between two suggestion requests. Servers rate limit them (Paper drops more than 15 per second by default).
     */
    private static final long REQUEST_INTERVAL_MILLIS = 100;
    /**
     * Running a command often changes what it suggests next time (deleting a home, leaving a team...), so its
     * suggestions are requested again shortly after the player runs it.
     */
    private static final long REFRESH_DELAY_MILLIS = 2000;
    private static final int MAX_VALUES = 200;
    /**
     * Answers come in one by one while the requests drain, so the commands are sent again once for all of them.
     */
    private static final long RESEND_DELAY_MILLIS = 1000;

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
    private final Set<String> refreshScheduled = new HashSet<>();
    private Runnable resendCommands = () -> {};
    private boolean resendScheduled;

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
     * Servers send the same command tree many times (joining, changing worlds, permission updates), so nothing is
     * requested when the arguments did not change.
     *
     * @param resendCommands sends the commands to the client again, with the values known at that time
     */
    public void setRequests(Map<String, String> requests, Runnable resendCommands) {
        session.ensureInEventLoop(() -> {
            this.resendCommands = resendCommands;
            if (this.requests.equals(requests)) {
                return;
            }
            this.requests = Map.copyOf(requests);
            this.values.keySet().retainAll(this.requests.keySet());
            this.pending.clear();
            this.queue.clear();
            requestAll();
        });
    }

    /**
     * Requests the suggestions of a command again in a moment, once for a burst of uses.
     *
     * @param command the command the player ran, without the leading slash
     */
    public void refreshSoon(String command) {
        int space = command.indexOf(' ');
        String prefix = "/" + (space < 0 ? command : command.substring(0, space)).toLowerCase(Locale.ROOT) + " ";
        session.ensureInEventLoop(() -> {
            if (!refreshScheduled.add(prefix)) {
                return;
            }
            session.scheduleInEventLoop(() -> {
                refreshScheduled.remove(prefix);
                for (Map.Entry<String, String> request : requests.entrySet()) {
                    if (request.getValue().startsWith(prefix) && !queue.contains(request.getKey())) {
                        queue.add(request.getKey());
                    }
                }
                if (!draining) {
                    drain();
                }
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
        if (!resendScheduled) {
            resendScheduled = true;
            session.scheduleInEventLoop(this::resendWhenDrained, RESEND_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        }
        return true;
    }

    private void resendWhenDrained() {
        if (draining) {
            session.scheduleInEventLoop(this::resendWhenDrained, RESEND_DELAY_MILLIS, TimeUnit.MILLISECONDS);
            return;
        }
        resendScheduled = false;
        resendCommands.run();
    }
}
