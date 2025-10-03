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

package org.geysermc.geyser.api.event.java;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.bedrock.SessionAcceptCodeOfConductEvent;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;

/**
 * Fired when the Java server sends a code of conduct during the configuration phase.
 * API users can listen to this event and tell Geyser the player has accepted the code of conduct before, which will result in the
 * code of conduct not being shown to the player.
 *
 * <p>Java clients cache this locally, but bedrock clients don't. Normally Geyser uses a simple JSON file to implement this,
 * but an alternative solution may be preferred when using multiple Geyser instances. Such a solution can be implemented through this event and {@link SessionAcceptCodeOfConductEvent}.</p>
 *
 * @see SessionAcceptCodeOfConductEvent
 * @since 2.9.0
 */
public final class ServerCodeOfConductEvent extends ConnectionEvent {
    private final String codeOfConduct;
    private boolean hasAccepted = false;
    
    public ServerCodeOfConductEvent(@NonNull GeyserConnection connection, String codeOfConduct) {
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
     * @return {@code true} if Geyser should not show the code of conduct to the player, because they have already accepted it
     * @since 2.9.0
     */
    public boolean accepted() {
        return hasAccepted;
    }

    /**
     * Sets {@link ServerCodeOfConductEvent#accepted()} to {@code true}.
     * @since 2.9.0
     */
    public void accept() {
        this.hasAccepted = true;
    }
}
