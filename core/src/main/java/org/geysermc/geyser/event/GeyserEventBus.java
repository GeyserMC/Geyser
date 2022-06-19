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

import net.kyori.event.EventSubscriber;
import net.kyori.event.SimpleEventBus;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.*;
import org.geysermc.geyser.api.extension.Extension;
import org.lanternpowered.lmbda.LambdaFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GeyserEventBus implements EventBus {
    private static final MethodHandles.Lookup CALLER = MethodHandles.lookup();

    private final SimpleEventBus<Event> bus = new SimpleEventBus<>(Event.class);

    @NonNull
    @Override
    public <T extends Event> EventSubscription<T> subscribe(@NonNull EventListener eventListener, @NonNull Class<T> eventClass, @NonNull Consumer<? super T> consumer) {
        return this.subscribe(eventClass, consumer, eventListener, Subscribe.PostOrder.NORMAL);
    }

    @Override
    public <T extends Event> void unsubscribe(@NonNull EventSubscription<T> subscription) {
        this.bus.unregister((AbstractEventSubscription<T>) subscription);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void register(@NonNull EventListener eventListener, @NonNull Object eventHolder) {
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

            try {
                Class<? extends Event> type = (Class<? extends Event>) method.getParameters()[0].getType();
                this.subscribe(type, eventHolder, LambdaFactory.createBiConsumer(CALLER.unreflect(method)), eventListener, subscribe.postOrder());
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void unregisterAll(@NonNull EventListener eventListener) {
        this.bus.unregister((Predicate<EventSubscriber<?>>) subscriber -> eventListener.equals(((AbstractEventSubscription<?>) subscriber).owner()));
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

    private <T extends Event> EventSubscription<T> subscribe(Class<T> eventClass, Consumer<? super T> handler, EventListener eventListener, Subscribe.PostOrder postOrder) {
        BaseEventSubscription<T> eventSubscription = new BaseEventSubscription<>(this, eventClass, eventListener, postOrder, handler);
        this.bus.register(eventClass, eventSubscription);
        return eventSubscription;
    }

    private <T extends Event> EventSubscription<T> subscribe(Class<T> eventClass, Object eventHolder, BiConsumer<Object, ? super T> handler, EventListener eventListener, Subscribe.PostOrder postOrder) {
        GeneratedEventSubscription<T> eventSubscription = new GeneratedEventSubscription<>(this, eventClass, eventListener, postOrder, eventHolder, handler);
        this.bus.register(eventClass, eventSubscription);
        return eventSubscription;
    }
}
