/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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
import org.geysermc.event.Cancellable;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;

import java.util.Set;

/**
 * Called when the Java server defines the commands available on the server.
 * <br>
 * This event is mapped to the existence of Brigadier on the server.
 */
public final class ServerDefineCommandsEvent extends ConnectionEvent implements Cancellable {
    private final Set<? extends CommandInfo> commands;
    private boolean cancelled;

    public ServerDefineCommandsEvent(@NonNull GeyserConnection connection, @NonNull Set<? extends CommandInfo> commands) {
        super(connection);
        this.commands = commands;
    }

    /**
     * A collection of commands sent from the server. Any element in this collection can be removed, but no element can
     * be added.
     *
     * @return a collection of the commands sent over
     */
    @NonNull
    public Set<? extends CommandInfo> commands() {
        return this.commands;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public interface CommandInfo {
        /**
         * Gets the name of the command.
         *
         * @return the name of the command
         */
        String name();

        /**
         * Gets the description of the command.
         *
         * @return the description of the command
         */
        String description();
    }
}
