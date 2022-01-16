/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.event;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.extension.Extension;

import java.util.function.Consumer;

/**
 * Represents a subscribed listener to a {@link Event}. Wraps around
 * the event and is capable of unsubscribing from the event or give
 * information about it.
 *
 * @param <T> the class of the event
 */
public interface EventSubscription<T extends Event> {

    /**
     * Gets the event class.
     *
     * @return the event class
     */
    @NonNull
    Class<T> eventClass();

    /**
     * Gets the consumer responsible for handling
     * this event.
     *
     * @return the consumer responsible for this event
     */
    @NonNull
    Consumer<? super T> eventConsumer();

    /**
     * Gets the {@link Extension} that owns this
     * event subscription.
     *
     * @return the extension that owns this subscription
     */
    @NonNull
    Extension owner();

    /**
     * Gets the priority of this event subscription.
     *
     * @return the priority of this event subscription
     */
    Subscribe.Priority priority();

    /**
     * Gets if this event subscription is active.
     *
     * @return if this event subscription is active
     */
    boolean isActive();

    /**
     * Unsubscribes from this event listener
     */
    void unsubscribe();

    /**
     * Invokes the given event
     *
     * @param event the event
     */
    void invoke(@NonNull T event) throws Throwable;
}
