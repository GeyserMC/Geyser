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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.event.Cancellable;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;

import java.util.concurrent.CompletableFuture;

/**
 * See {@link ServerSpawnEntityEvent} and {@link SessionAttachParrotsEvent}
 */
public abstract class SessionSpawnEntityEvent extends ConnectionEvent implements Cancellable {

    public SessionSpawnEntityEvent(@NonNull GeyserConnection connection) {
        super(connection);
    }

    /**
     * Gets the entity definition sent to the connection when the entity is spawned.
     *
     * @return the entity definition sent to the connection when the entity is spawned
     */
    public abstract @Nullable GeyserEntityDefinition entityDefinition();

    /**
     * Sets the entity definition sent to the connection when the entity is spawned.
     * This entity definition MUST have been registered in the {@link GeyserDefineEntitiesEvent} before
     * using it here!
     *
     * @param entityDefinition the entity definition sent to the connection when the entity is spawned
     */
    public abstract void entityDefinition(@Nullable GeyserEntityDefinition entityDefinition);

    /**
     * @return the GeyserEntity once it is created, or null if the spawn event is cancelled
     */
    public abstract CompletableFuture<@Nullable GeyserEntity> futureEntity();
}
