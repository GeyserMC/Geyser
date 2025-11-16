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

package org.geysermc.geyser.api.network.message;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.network.MessageDirection;

/**
 * Represents a handler for processing messages.
 *
 * @param <T> the type of message to handle
 */
@FunctionalInterface
public interface MessageHandler<T extends Message<? extends MessageBuffer>> {

    /**
     * Handles the given message in the specified direction.
     *
     * @param message the message to handle
     * @param direction the direction of the message
     * @return the state after handling the message
     */
    @NonNull
    State handle(@NonNull T message, @NonNull MessageDirection direction);

    /**
     * A message handler that belongs to a specific side (clientbound or serverbound).
     *
     * @param <T> the type of message to handle
     */
    interface Sided<T extends Message<? extends MessageBuffer>> {

        /**
         * Handles the given message in the specified direction.
         *
         * @param message the message to handle
         * @return the state after handling the message
         */
        @NonNull
        State handle(@NonNull T message);
    }

    /**
     * Represents the state after handling a message.
     */
    enum State {
        /**
         * The message was handled and should not be processed further.
         */
        HANDLED,
        /**
         * The message was not handled and should be passed through for further processing.
         */
        UNHANDLED,
        /**
         * Indicates that the message has been modified but should still be
         * passed through to the next handler or processing step.
         */
        MODIFIED
    }
}
