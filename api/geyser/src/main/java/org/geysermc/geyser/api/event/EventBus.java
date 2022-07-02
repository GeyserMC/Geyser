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
import org.geysermc.geyser.api.extension.Extension;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Represents a bus capable of subscribing
 * or "listening" to events and firing them.
 */
public interface EventBus {
    /**
     * Subscribes to the given event see {@link EventSubscription}.
     *
     * The difference between this method and {@link ExtensionEventBus#subscribe(Class, Consumer)}
     * is that this method takes in an extension parameter which allows for
     * the event to be unsubscribed upon extension disable and reloads.
     *
     * @param eventListener the event listener to subscribe the event to
     * @param eventClass the class of the event
     * @param consumer the consumer for handling the event
     * @param <T> the event class
     * @return the event subscription
     */
    @NonNull
    <T extends Event> EventSubscription<T> subscribe(@NonNull EventListener eventListener, @NonNull Class<T> eventClass, @NonNull Consumer<? super T> consumer);

    /**
     * Unsubscribes the given {@link EventSubscription}.
     *
     * @param subscription the event subscription
     */
    <T extends Event> void unsubscribe(@NonNull EventSubscription<T> subscription);

    /**
     * Registers events for the given listener.
     *
     * @param eventListener the event listener registering the event
     * @param eventHolder the listener
     */
    void register(@NonNull EventListener eventListener, @NonNull Object eventHolder);

    /**
     * Unregisters all events from a given {@link EventListener}.
     *
     * @param eventListener the listener
     */
    void unregisterAll(@NonNull EventListener eventListener);

    /**
     * Fires the given {@link Event} and returns the result.
     *
     * @param event the event to fire
     *
     * @return true if the event successfully fired
     */
    boolean fire(@NonNull Event event);

    /**
     * Gets the subscriptions for the given event class.
     *
     * @param eventClass the event class
     * @param <T> the value
     * @return the subscriptions for the event class
     */
    @NonNull
    <T extends Event> Set<EventSubscription<T>> subscriptions(@NonNull Class<T> eventClass);
}
