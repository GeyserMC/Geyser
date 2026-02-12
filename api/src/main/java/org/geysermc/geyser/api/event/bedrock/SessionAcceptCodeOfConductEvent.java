/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.event.java.ServerCodeOfConductEvent;

/**
 * Fired when a player accepts a code of conduct sent by the Java server. API users can listen to this event
 * to store the acceptance in a cache, and tell Geyser not to do so.
 *
 * <p>Java clients cache acceptance locally, but bedrock clients don't. Normally Geyser uses a simple JSON file to implement this,
 * but an alternative solution may be preferred when using multiple Geyser instances. Such a solution can be implemented through this event and {@link ServerCodeOfConductEvent}.</p>
 *
 * @see ServerCodeOfConductEvent
 * @since 2.9.0
 */
public class SessionAcceptCodeOfConductEvent extends ConnectionEvent {
    private final String codeOfConduct;
    private boolean skipSaving = false;

    public SessionAcceptCodeOfConductEvent(@NonNull GeyserConnection connection, String codeOfConduct) {
        super(connection);
        this.codeOfConduct = codeOfConduct;
    }

    /**
     * @return the code of conduct sent by the server
     * @since 2.9.0
     */
    public String codeOfConduct() {
        return codeOfConduct;
    }

    /**
     * @return {@code true} if Geyser should not save the acceptance of the code of conduct in its own cache (through a JSON file), because it was saved elsewhere
     * @since 2.9.0
     */
    public boolean shouldSkipSaving() {
        return skipSaving;
    }

    /**
     * Sets {@link SessionAcceptCodeOfConductEvent#shouldSkipSaving()} to {@code true}.
     * @since 2.9.0
     */
    public void skipSaving() {
        this.skipSaving = true;
    }
}
