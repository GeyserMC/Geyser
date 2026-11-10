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

package org.geysermc.geyser.api.event.connection;

import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.api.waypoint.CustomWaypointStyle;
import org.geysermc.geyser.api.waypoint.CustomWaypointStyleRegisterException;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

/**
 * Called once for every {@link GeyserConnection} to register custom waypoint styles. Custom waypoint styles must be registered through this event.
 *
 * <p>{@link CustomWaypointStyle}s may be registered multiple times for different Java Edition waypoint style {@link Identifier}s, however, each Java Edition waypoint style
 * may only have one {@link CustomWaypointStyle}.</p>
 *
 * @see CustomWaypointStyle
 * @since 2.11.0
 */
@ApiStatus.NonExtendable
public abstract class SessionDefineCustomWaypointsEvent extends ConnectionEvent {

    @ApiStatus.Internal
    public SessionDefineCustomWaypointsEvent(GeyserConnection connection) {
        super(connection);
    }

    /**
     * Returns a map of all the currently registered custom waypoint styles for this {@link GeyserConnection}.
     *
     * @return an unmodifiable map of all currently registered custom waypoint styles
     * @since 2.11.0
     */
    public abstract Map<Identifier, CustomWaypointStyle> customWaypointStyles();

    /**
     * Registers a {@link CustomWaypointStyle} for this {@link GeyserConnection}, for the given Java Edition waypoint style {@code identifier}.
     *
     * <p>The given {@link CustomWaypointStyle} will be used for every waypoint that uses the given {@code identifier} as waypoint style. Vanilla
     * waypoint styles, like {@code minecraft:default}, may be overridden.</p>
     *
     * <p>When a waypoint style is already registered for the given {@code identifier}, this method will fail without throwing an exception, but an error will be logged.</p>
     *
     * @param identifier the identifier of the waypoint style on Java Edition
     * @param style the {@link CustomWaypointStyle}
     * @throws CustomWaypointStyleRegisterException when an error occurred while registering the custom waypoint style
     * @since 2.11.0
     */
    public abstract void register(Identifier identifier, CustomWaypointStyle style);
}
