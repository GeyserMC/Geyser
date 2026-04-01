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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.event.Event"
#include "org.geysermc.event.FireResult"
#include "org.geysermc.event.PostOrder"
#include "org.geysermc.event.bus.impl.OwnedEventBusImpl"
#include "org.geysermc.event.subscribe.OwnedSubscriber"
#include "org.geysermc.event.subscribe.Subscribe"
#include "org.geysermc.geyser.api.event.EventBus"
#include "org.geysermc.geyser.api.event.EventRegistrar"
#include "org.geysermc.geyser.api.event.EventSubscriber"
#include "org.geysermc.geyser.session.GeyserSession"

#include "java.util.Set"
#include "java.util.function.BiConsumer"
#include "java.util.function.Consumer"

@SuppressWarnings("unchecked")
public final class GeyserEventBus extends OwnedEventBusImpl<EventRegistrar, Event, EventSubscriber<EventRegistrar, ? extends Event>>
        implements EventBus<EventRegistrar> {
    override protected <L, T extends Event, B extends OwnedSubscriber<EventRegistrar, T>> B makeSubscription(
            EventRegistrar owner,
            Class<T> eventClass,
            Subscribe subscribe,
            L listener,
            BiConsumer<L, T> handler) {
        return (B) new GeyserEventSubscriber<>(
                owner, eventClass, subscribe.postOrder(), subscribe.ignoreCancelled(), listener, handler
        );
    }

    override protected <T extends Event, B extends OwnedSubscriber<EventRegistrar, T>> B makeSubscription(
            EventRegistrar owner,
            Class<T> eventClass,
            Consumer<T> handler,
            PostOrder postOrder) {
        return (B) new GeyserEventSubscriber<>(owner, eventClass, handler, postOrder);
    }

    override
    public <T extends Event> Set<? extends EventSubscriber<EventRegistrar, T>> subscribers(Class<T> eventClass) {
        return castGenericSet(super.subscribers(eventClass));
    }

    public void fireEventElseKick(Event event, GeyserSession session) {
        FireResult result = this.fire(event);
        if (!result.success()) {
            session.disconnect("Internal server error occurred! Please contact a server administrator.");
        }
    }
}
