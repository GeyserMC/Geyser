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
import org.geysermc.event.Cancellable;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.definition.JavaEntityType;
import org.geysermc.geyser.api.event.bedrock.SessionSpawnEntityEvent;

import java.util.UUID;

/**
 * Called when the downstream server spawns a non-player entity.
 */
public abstract class ServerSpawnEntityEvent extends SessionSpawnEntityEvent implements Cancellable {

    public ServerSpawnEntityEvent(@NonNull GeyserConnection connection) {
        super(connection);
    }

    /**
     * Gets the entity id of the entity being spawned.
     *
     * @return the entity id of the entity being spawned
     */
    public abstract int entityId();

    /**
     * Gets the uuid of the entity being spawned.
     *
     * @return the uuid of the entity being spawned
     */
    public abstract @NonNull UUID uuid();

    /**
     * Gets the Java entity type sent by the server
     *
     * @return the Java edition entity type of the entity being spawned
     */
    public abstract @NonNull JavaEntityType entityType();
}
