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

package org.geysermc.geyser.extension.event;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.event.Event;
import org.geysermc.event.FireResult;
import org.geysermc.event.PostOrder;
import org.geysermc.event.subscribe.Subscriber;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.EventSubscriber;
import org.geysermc.geyser.api.event.ExtensionEventBus;
import org.geysermc.geyser.api.extension.Extension;

import java.util.Set;
import java.util.function.Consumer;

public record GeyserExtensionEventBus(EventBus<EventRegistrar> eventBus, Extension extension) implements ExtensionEventBus {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void unsubscribe(@NonNull EventSubscriber<Extension, ? extends Event> subscription) {
        eventBus.unsubscribe((EventSubscriber) subscription);
    }

    @Override
    public FireResult fire(@NonNull Event event) {
        return eventBus.fire(event);
    }

    @Override
    public FireResult fireSilently(@NonNull Event event) {
        return eventBus.fireSilently(event);
    }

    @Override
    public @NonNull <T extends Event> Set<? extends EventSubscriber<EventRegistrar, T>> subscribers(@NonNull Class<T> eventClass) {
        return eventBus.subscribers(eventClass);
    }

    @Override
    public void register(@NonNull Object listener) {
        eventBus.register(extension, listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event, U extends Subscriber<T>> @NonNull U subscribe(
            @NonNull Class<T> eventClass, @NonNull Consumer<T> consumer) {
        return eventBus.subscribe(extension, eventClass, consumer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event, U extends Subscriber<T>> @NonNull U subscribe(
            @NonNull Class<T> eventClass,
            @NonNull Consumer<T> consumer,
            @NonNull PostOrder postOrder
    ) {
        return eventBus.subscribe(extension, eventClass, consumer, postOrder);
    }

    @Override
    public void unregisterAll() {
        eventBus.unregisterAll(extension);
    }
}
