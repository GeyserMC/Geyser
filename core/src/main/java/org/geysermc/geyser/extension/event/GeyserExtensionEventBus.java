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
import org.geysermc.geyser.api.event.*;
import org.geysermc.geyser.api.extension.Extension;

import java.util.Set;
import java.util.function.Consumer;

public record GeyserExtensionEventBus(EventBus eventBus,
                                      Extension extension) implements ExtensionEventBus {
    @NonNull
    @Override
    public <T extends Event> EventSubscription<T> subscribe(@NonNull Class<T> eventClass, @NonNull Consumer<? super T> consumer) {
        return this.eventBus.subscribe(this.extension, eventClass, consumer);
    }

    @Override
    public void register(@NonNull Object eventHolder) {
        this.eventBus.register(this.extension, eventHolder);
    }

    @Override
    public void unregisterAll() {
        this.eventBus.unregisterAll(this.extension);
    }

    @NonNull
    @Override
    public <T extends Event> EventSubscription<T> subscribe(@NonNull EventListener eventListener, @NonNull Class<T> eventClass, @NonNull Consumer<? super T> consumer) {
        return this.eventBus.subscribe(eventListener, eventClass, consumer);
    }

    @Override
    public <T extends Event> void unsubscribe(@NonNull EventSubscription<T> subscription) {
        this.eventBus.unsubscribe(subscription);
    }

    @Override
    public void register(@NonNull EventListener eventListener, @NonNull Object eventHolder) {
        this.eventBus.register(eventListener, eventHolder);
    }

    @Override
    public void unregisterAll(@NonNull EventListener eventListener) {
        this.eventBus.unregisterAll(eventListener);
    }

    @Override
    public boolean fire(@NonNull Event event) {
        return this.eventBus.fire(event);
    }

    @NonNull
    @Override
    public <T extends Event> Set<EventSubscription<T>> subscriptions(@NonNull Class<T> eventClass) {
        return this.eventBus.subscriptions(eventClass);
    }
}
