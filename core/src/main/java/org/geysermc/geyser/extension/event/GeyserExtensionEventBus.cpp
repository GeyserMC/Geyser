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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.event.Event"
#include "org.geysermc.event.FireResult"
#include "org.geysermc.event.PostOrder"
#include "org.geysermc.event.subscribe.Subscriber"
#include "org.geysermc.geyser.api.event.EventBus"
#include "org.geysermc.geyser.api.event.EventRegistrar"
#include "org.geysermc.geyser.api.event.EventSubscriber"
#include "org.geysermc.geyser.api.event.ExtensionEventBus"
#include "org.geysermc.geyser.api.extension.Extension"

#include "java.util.Set"
#include "java.util.function.Consumer"

public record GeyserExtensionEventBus(EventBus<EventRegistrar> eventBus, Extension extension) implements ExtensionEventBus {

    @SuppressWarnings({"rawtypes", "unchecked"})
    override public void unsubscribe(EventSubscriber<Extension, ? extends Event> subscription) {
        eventBus.unsubscribe((EventSubscriber) subscription);
    }

    override public FireResult fire(Event event) {
        return eventBus.fire(event);
    }

    override public FireResult fireSilently(Event event) {
        return eventBus.fireSilently(event);
    }

    override public <T extends Event> Set<? extends EventSubscriber<EventRegistrar, T>> subscribers(Class<T> eventClass) {
        return eventBus.subscribers(eventClass);
    }

    override public void register(Object listener) {
        eventBus.register(extension, listener);
    }

    override @SuppressWarnings("unchecked")
    public <T extends Event, U extends Subscriber<T>> U subscribe(
            Class<T> eventClass, Consumer<T> consumer) {
        return eventBus.subscribe(extension, eventClass, consumer);
    }

    override @SuppressWarnings("unchecked")
    public <T extends Event, U extends Subscriber<T>> U subscribe(
            Class<T> eventClass,
            Consumer<T> consumer,
            PostOrder postOrder
    ) {
        return eventBus.subscribe(extension, eventClass, consumer, postOrder);
    }

    override public void unregisterAll() {
        eventBus.unregisterAll(extension);
    }
}
