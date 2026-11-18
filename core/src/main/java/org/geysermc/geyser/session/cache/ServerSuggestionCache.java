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
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Bedrock clients never ask the server for command suggestions, so arguments whose suggestions
 * are computed by the Java server (the "minecraft:ask_server" suggestion type, used by most plugin commands)
 * would show no options. Geyser asks the Java server on behalf of the player and sends the commands again
 * once the answers are in, with the answers as enums.
 * <p>
 * Plugin commands usually take all their arguments as one string, so the server is also asked what comes after each
 * suggested word ("/home " then "/home set "), up to {@link #MAX_ARGUMENTS} arguments.
 */
public final class ServerSuggestionCache {
    /**
     * Time between two suggestion requests, to not flood the server while a player joins.
     */
    private static final long REQUEST_INTERVAL_MILLIS = 40;
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
    /**
     * While the next arguments are still asked, the commands are sent again at most this often so they show up early.
     */
    private static final long PROGRESS_RESEND_MILLIS = 3000;
    private static final int MAX_ARGUMENTS = 4;
    /**
     * Only short lists look like sub commands; asking what follows every entry of a long list (all commands for /help,
     * all items...) would cost many requests for little use.
     */
    private static final int MAX_FOLLOW_UP_VALUES = 32;
    /**
     * Upper bound of requests after the first argument for one command tree.
     */
    private static final int MAX_FOLLOW_UPS = 256;
    /**
     * Java names have up to 16 characters, plus the prefix proxies like Floodgate add to Bedrock players
     */
    private static final int MAX_NAME_LENGTH = 20;

    private final GeyserSession session;
    /**
     * Command texts whose suggestions fill the first argument, e.g. "/home "
     */
    private Set<String> requests = Set.of();
    /**
     * Command text sent to the Java server, for both levels, to the suggestions it answered
     */
    private final Map<String, Set<String>> values = new HashMap<>();
    /**
     * Aliases answer the same as the command they point to; only the first one asks for the second level
     */
    private final Map<String, String> sameAs = new HashMap<>();
    private final Int2ObjectMap<String> pending = new Int2ObjectOpenHashMap<>();
    private final Deque<String> queue = new ArrayDeque<>();
    private int nextTransactionId = 1;
    private int followUps;
    private boolean draining;
    private final Set<String> refreshScheduled = new HashSet<>();
    private Runnable resendCommands = () -> {};
    private boolean resendScheduled;
    private long lastResend;

    public ServerSuggestionCache(GeyserSession session) {
        this.session = session;
    }

    /**
     * @param text the command text up to the argument, e.g. "/home "
     * @return the last suggestions received for it, empty when the server has not answered yet
     */
    public Set<String> suggestions(String text) {
        return values.getOrDefault(text, Set.of());
    }

    /**
     * @return the suggestions for the argument after {@code value}, e.g. "/home " and "set"
     */
    public Set<String> suggestionsAfter(String text, String value) {
        return suggestions(canonical(text + value + " "));
    }

    /**
     * @return the text an alias' answers are kept under
     */
    private String canonical(String text) {
        for (Map.Entry<String, String> alias : sameAs.entrySet()) {
            if (text.startsWith(alias.getKey())) {
                return alias.getValue() + text.substring(alias.getKey().length());
            }
        }
        return text;
    }

    /**
     * @return 1 for the first argument of a command, 2 for the one after it...
     */
    private int argument(String text) {
        for (String request : requests) {
            if (text.startsWith(request)) {
                return 1 + (int) text.substring(request.length()).chars().filter(c -> c == ' ').count();
            }
        }
        return MAX_ARGUMENTS;
    }

    /**
     * Bedrock completes player names by itself and keeps them current, so a list of players is better sent
     * as a target than as an enum that gets outdated as soon as someone joins.
     * Plugins often suggest every player that ever joined, not only the online ones; such a list still has the
     * player's own name.
     */
    public boolean onlyPlayers(Set<String> suggestions) {
        if (suggestions.isEmpty()) {
            return false;
        }
        boolean hasOwnName = suggestions.contains(session.getPlayerEntity().getUsername());
        for (String suggestion : suggestions) {
            if (suggestion.length() > MAX_NAME_LENGTH) {
                return false;
            }
            if (!hasOwnName && session.getEntityCache().getPlayersByName(suggestion).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Replaces the arguments to fill with server suggestions and requests all of them.
     * Servers send the same command tree many times (joining, changing worlds, permission updates), so nothing is
     * requested when the arguments did not change.
     *
     * @param requests command texts up to each argument, e.g. "/home "
     * @param resendCommands sends the commands to the client again, with the suggestions known at that time
     */
    public void setRequests(Set<String> requests, Runnable resendCommands) {
        session.ensureInEventLoop(() -> {
            this.resendCommands = resendCommands;
            if (this.requests.equals(requests)) {
                return;
            }
            this.requests = Set.copyOf(requests);
            this.values.keySet().removeIf(text -> !isWanted(text));
            this.sameAs.clear();
            this.pending.clear();
            this.queue.clear();
            this.followUps = 0;
            for (String text : this.requests) {
                queue.add(text);
            }
            if (!draining) {
                drain();
            }
        });
    }

    private boolean isWanted(String text) {
        if (requests.contains(text)) {
            return true;
        }
        for (String request : requests) {
            if (text.startsWith(request)) {
                return true;
            }
        }
        return false;
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
                Set<String> texts = new LinkedHashSet<>();
                for (String request : requests) {
                    if (request.startsWith(prefix)) {
                        texts.add(request);
                    }
                }
                for (String text : values.keySet()) {
                    if (text.startsWith(prefix)) {
                        texts.add(text);
                    }
                }
                for (String text : texts) {
                    if (!queue.contains(text)) {
                        queue.add(text);
                    }
                }
                if (!draining) {
                    drain();
                }
            }, REFRESH_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        });
    }

    private void drain() {
        String text = queue.poll();
        if (text == null) {
            draining = false;
            return;
        }
        draining = true;
        int transactionId = nextTransactionId++;
        // Paper does not answer when there is nothing to suggest, so an older request for the same text may still wait
        pending.values().remove(text);
        pending.put(transactionId, text);
        session.sendDownstreamGamePacket(new ServerboundCommandSuggestionPacket(transactionId, text));
        session.scheduleInEventLoop(this::drain, REQUEST_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * @return false if the response was not requested by this cache (a Java client behind Geyser never asks)
     */
    public boolean handleResponse(int transactionId, String[] matches) {
        String text = pending.remove(transactionId);
        if (text == null) {
            return false;
        }
        if (!isWanted(text)) {
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
        if (argument(text) < MAX_ARGUMENTS) {
            askWhatFollows(text, newValues);
        }
        if (newValues.equals(values.getOrDefault(text, Set.of()))) {
            return true;
        }
        values.put(text, newValues);
        if (!resendScheduled) {
            resendScheduled = true;
            session.scheduleInEventLoop(this::resendWhenReady, RESEND_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        }
        return true;
    }

    private void askWhatFollows(String text, Set<String> newValues) {
        boolean firstArgument = requests.contains(text);
        if (firstArgument) {
            sameAs.remove(text);
        }
        if (newValues.size() > MAX_FOLLOW_UP_VALUES || onlyPlayers(newValues)) {
            return;
        }
        for (String other : firstArgument ? requests : Set.<String>of()) {
            // shortcut: an alias is recognised by the same first answers, so two commands with the same sub commands
            // but different arguments after them share the second one. Good enough for suggestions.
            if (!other.equals(text) && !sameAs.containsKey(other) && newValues.equals(values.get(other))) {
                sameAs.put(text, other);
                return;
            }
        }
        for (String value : newValues) {
            String next = text + value + " ";
            if (followUps < MAX_FOLLOW_UPS && !values.containsKey(next) && !queue.contains(next)) {
                followUps++;
                queue.add(next);
            }
        }
        if (!draining) {
            drain();
        }
    }

    /**
     * Sends the commands once the first arguments are all asked, then every few seconds while the next ones are,
     * and once more when everything is.
     * Unanswered requests are not waited for: Paper does not answer when there is nothing to suggest.
     */
    private void resendWhenReady() {
        boolean firstLevelQueued = queue.stream().anyMatch(requests::contains);
        long now = System.currentTimeMillis();
        if (draining && (firstLevelQueued || now - lastResend < PROGRESS_RESEND_MILLIS)) {
            session.scheduleInEventLoop(this::resendWhenReady, RESEND_DELAY_MILLIS, TimeUnit.MILLISECONDS);
            return;
        }
        lastResend = now;
        resendScheduled = false;
        resendCommands.run();
    }
}
