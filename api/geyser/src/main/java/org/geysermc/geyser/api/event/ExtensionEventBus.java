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

import java.util.function.Consumer;

/**
 * An {@link EventBus} with additional methods that implicitly
 * set the extension instance.
 *
 */
public interface ExtensionEventBus extends EventBus {

    /**
     * Subscribes to the given event see {@link EventSubscription}.
     *
     * @param eventClass the class of the event
     * @param consumer the consumer for handling the event
     * @param <T> the event class
     * @return the event subscription
     */
    @NonNull
    <T extends Event> EventSubscription<T> subscribe(@NonNull Class<T> eventClass, @NonNull Consumer<? super T> consumer);

    /**
     * Registers events for the given listener.
     *
     * @param eventHolder the listener
     */
    void register(@NonNull Object eventHolder);

    /**
     * Unregisters all events for this extension.
     */
    void unregisterAll();
}
