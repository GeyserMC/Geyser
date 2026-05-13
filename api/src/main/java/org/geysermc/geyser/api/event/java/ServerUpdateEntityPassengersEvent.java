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

package org.geysermc.geyser.api.event.java;

import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;

/**
 * This event is called when an entity's passengers are updated.
 * To avoid de-syncs, you cannot cancel this event.
 */
public abstract class ServerUpdateEntityPassengersEvent extends ConnectionEvent {

    /**
     * The vehicle entity that gets a passenger update.
     *
     * @return the vehicle entity
     */
    public abstract GeyserEntity vehicle();

    public ServerUpdateEntityPassengersEvent(GeyserConnection connection) {
        super(connection);
    }

    public abstract static class Mount extends ServerUpdateEntityPassengersEvent {
        public Mount(GeyserConnection connection) {
            super(connection);
        }

        /**
         * @return the passenger that was added to the vehicle
         */
        public abstract GeyserEntity addedPassenger();
    }

    public abstract static class Dismount extends ServerUpdateEntityPassengersEvent {
        public Dismount(GeyserConnection connection) {
            super(connection);
        }

        /**
         * @return the passenger that was removed from the vehicle
         */
        public abstract GeyserEntity removedPassenger();
    }
}
