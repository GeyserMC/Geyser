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

import org.geysermc.event.Cancellable;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.EntityData;
import org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.event.java.ServerAttachParrotsEvent;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Base event for entity spawning visible to a specific Bedrock connection.
 * Specific subtypes are {@link ServerSpawnEntityEvent} (non-player entities) and
 * {@link ServerAttachParrotsEvent} (shoulder parrots).
 * <p>
 * The event is cancellable; cancelling it suppresses spawning the entity on the Bedrock client.
 * Setting the {@link #definition(GeyserEntityDefinition) definition} to
 * {@code null} has the same effect.
 *
 * @since 2.11.0
 */
@ApiStatus.Experimental
public abstract class SessionSpawnEntityEvent extends ConnectionEvent implements Cancellable {

    @ApiStatus.Internal
    public SessionSpawnEntityEvent(GeyserConnection connection) {
        super(connection);
    }

    /**
     * Returns the Bedrock entity definition that will be sent to the Bedrock client.
     * Setting a definition to null will cancel this entity spawn!
     *
     * @return the entity definition, or {@code null} if none is currently set
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public abstract @Nullable GeyserEntityDefinition definition();

    /**
     * Overrides the Bedrock entity definition when spawning this entity on the Bedrock client.
     * <p>
     * The supplied definition must have been previously registered in the
     * {@link GeyserDefineEntitiesEvent}; passing an unregistered definition will throw
     * {@link IllegalStateException}. Passing {@code null} cancels the spawn for this connection.
     *
     * @param definition the replacement entity definition, or {@code null} to suppress the spawn
     * @throws IllegalStateException if the provided definition has not been registered
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public abstract void definition(@Nullable GeyserEntityDefinition definition);

    /**
     * Registers a callback that is invoked with the {@link GeyserEntity} instance just before the entity is spawned.
     * This means that {@link EntityData#byJavaId}, {@link EntityData#byUuid}, and related lookups
     * will <em>not</em> find the entity from within the consumer.
     * <p>
     * Use this callback to apply initial overrides such as entity data values (scale, width,
     * height, hitboxes) or entity properties before the spawn packet is sent to the client.
     * If multiple consumers are registered, they run in the order they were added.
     * <p>
     * The consumer is not called if the event is cancelled.
     *
     * @param consumer the callback receiving the freshly created entity
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public abstract void preSpawnConsumer(Consumer<GeyserEntity> consumer);
}
