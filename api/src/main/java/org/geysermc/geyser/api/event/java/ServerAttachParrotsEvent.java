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

import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.type.player.GeyserPlayerEntity;
import org.geysermc.geyser.api.event.bedrock.SessionSpawnEntityEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Called when the Java server attaches parrots to a player.
 *
 * @since 2.11.0
 */
@ApiStatus.Experimental
public abstract class ServerAttachParrotsEvent extends SessionSpawnEntityEvent {

    @ApiStatus.Internal
    public ServerAttachParrotsEvent(GeyserConnection connection) {
        super(connection);
    }

    /**
     * The player for which the Java server attached parrots.
     *
     * @return the player with bird friends
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public abstract GeyserPlayerEntity player();

    /**
     * The variant of the parrot.
     *
     * @return the parrot variant
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public abstract int variant();

    /**
     * Whether this parrot is on the right shoulder of the player.
     *
     * @return true if parrot is on the right shoulder, left otherwise
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public abstract boolean right();
}
