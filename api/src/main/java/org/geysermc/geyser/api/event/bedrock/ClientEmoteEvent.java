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

package org.geysermc.geyser.api.event.bedrock;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.event.Cancellable;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Called whenever a Bedrock player performs an emote on their end, before it is broadcasted to the rest of the server.
 *
 * @since 2.1.0
 */
public final class ClientEmoteEvent extends ConnectionEvent implements Cancellable {
    private final String emoteId;
    private boolean silent;
    private boolean cancelled;

    @ApiStatus.Internal
    public ClientEmoteEvent(@NonNull GeyserConnection connection, @NonNull String emoteId) {
        super(connection);
        this.emoteId = emoteId;
    }

    /**
     * The emote ID that the Bedrock player is attempting to perform. It is sent
     * directly by the client, and is not guaranteed to be a valid emote ID.
     *
     * @return the emote ID requested by the player
     * @since 2.1.0
     */
    @NonNull
    public String emoteId() {
        return emoteId;
    }

    /**
     * Whether the emote should be played silently, or announced in the server chat
     *
     * @return whether the emote should be played silently
     * @since 2.9.6
     */
    public boolean silent() {
        return silent;
    }

    /**
     * Whether the emote should be played silently, or announced in the server chat
     * @see #silent()
     *
     * @param silent if true, the emote will not be announced in the server chat
     * @since 2.9.6
     */
    public void silent(boolean silent) {
        this.silent = silent;
    }

    /**
     * Whether this event is cancelled. A Bedrock player will still play this emote on its end
     * even if this event is cancelled, but other Bedrock players will not see.
     *
     * @return the cancel status of this event
     * @since 2.1.0
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the cancel status of this event. If this event is canceled, the emote will not be played to other players.
     *
     * @param cancelled whether this event is cancelled
     * @since 2.1.0
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
