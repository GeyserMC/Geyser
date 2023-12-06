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

package org.geysermc.geyser.event;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.event.Event;
import org.geysermc.event.PostOrder;
import org.geysermc.event.bus.impl.OwnedEventBusImpl;
import org.geysermc.event.subscribe.OwnedSubscriber;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.EventSubscriber;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public final class GeyserEventBus extends OwnedEventBusImpl<EventRegistrar, Event, EventSubscriber<EventRegistrar, ? extends Event>>
        implements EventBus<EventRegistrar> {
    @Override
    protected <L, T extends Event, B extends OwnedSubscriber<EventRegistrar, T>> B makeSubscription(
            @NonNull EventRegistrar owner,
            @NonNull Class<T> eventClass,
            @NonNull Subscribe subscribe,
            @NonNull L listener,
            @NonNull BiConsumer<L, T> handler) {
        return (B) new GeyserEventSubscriber<>(
                owner, eventClass, subscribe.postOrder(), subscribe.ignoreCancelled(), listener, handler
        );
    }

    @Override
    protected <T extends Event, B extends OwnedSubscriber<EventRegistrar, T>> B makeSubscription(
            @NonNull EventRegistrar owner,
            @NonNull Class<T> eventClass,
            @NonNull Consumer<T> handler,
            @NonNull PostOrder postOrder) {
        return (B) new GeyserEventSubscriber<>(owner, eventClass, handler, postOrder);
    }

    @Override
    @NonNull
    public <T extends Event> Set<? extends EventSubscriber<EventRegistrar, T>> subscribers(@NonNull Class<T> eventClass) {
        return castGenericSet(super.subscribers(eventClass));
    }
}
