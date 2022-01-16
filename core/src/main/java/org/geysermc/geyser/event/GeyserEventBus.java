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

import net.kyori.event.SimpleEventBus;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.event.Event;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventSubscription;
import org.geysermc.geyser.api.event.Subscribe;
import org.geysermc.geyser.api.extension.Extension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GeyserEventBus implements EventBus {
    private final SimpleEventBus<Event> bus = new SimpleEventBus<>(Event.class);

    @NonNull
    @Override
    public <T extends Event> EventSubscription<T> subscribe(@NonNull Class<T> eventClass, @NonNull Consumer<? super T> consumer) {
        return this.subscribe(eventClass, consumer, null, Subscribe.Priority.NORMAL);
    }

    @NonNull
    @Override
    public <T extends Event> EventSubscription<T> subscribe(@NonNull Extension extension, @NonNull Class<T> eventClass, @NonNull Consumer<? super T> consumer) {
        return this.subscribe(eventClass, consumer, extension, Subscribe.Priority.NORMAL);
    }

    @Override
    public <T extends Event> void unsubscribe(@NonNull EventSubscription<T> subscription) {
        this.bus.unregister((GeyserEventSubscription<T>) subscription);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void register(@NonNull Extension extension, @NonNull Object eventHolder) {
        for (Method method : eventHolder.getClass().getMethods()) {
            if (!method.isAnnotationPresent(Subscribe.class)) {
                continue;
            }

            if (method.getParameterCount() > 1) {
                continue;
            }

            if (!Event.class.isAssignableFrom(method.getParameters()[0].getType())) {
                continue;
            }

            Subscribe subscribe = method.getAnnotation(Subscribe.class);
            this.subscribe((Class<? extends Event>) method.getParameters()[0].getType(), (event) -> {
                try {
                    method.invoke(eventHolder, event);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            }, extension, subscribe.priority());
        }
    }

    @Override
    public boolean fire(@NonNull Event event) {
        return this.bus.post(event).wasSuccessful();
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends Event> Set<EventSubscription<T>> subscriptions(@NonNull Class<T> eventClass) {
        return bus.subscribers().values()
                .stream()
                .filter(sub -> sub instanceof EventSubscription && ((EventSubscription<?>) sub).eventClass().isAssignableFrom(eventClass))
                .map(sub -> ((EventSubscription<T>) sub))
                .collect(Collectors.toSet());
    }

    private <T extends Event> EventSubscription<T> subscribe(Class<T> eventClass, Consumer<? super T> handler, Extension extension, Subscribe.Priority priority) {
        GeyserEventSubscription<T> eventSubscription = new GeyserEventSubscription<>(this, eventClass, handler, extension, priority);
        this.bus.register(eventClass, eventSubscription);
        return eventSubscription;
    }
}
