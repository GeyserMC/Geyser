/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.event.handlers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.connector.event.Cancellable;
import org.geysermc.connector.event.EventManager;
import org.geysermc.connector.event.GeyserEvent;

import java.util.function.BiConsumer;


/**
 * Provides an event handler for an annotated method
 */
@Getter
public class LambdaEventHandler<T extends GeyserEvent> extends EventHandler<T> {
    private final BiConsumer<T, EventHandler<T>> consumer;
    private final int priority;
    private final boolean ignoreCancelled;

    public LambdaEventHandler(EventManager manager, Class<T> cls, BiConsumer<T, EventHandler<T>> consumer, int priority, boolean ignoreCancelled) {
        super(manager, cls);
        this.consumer = consumer;
        this.priority = priority;
        this.ignoreCancelled = ignoreCancelled;
    }

    @Override
    public void execute(T event) {
        if (event instanceof Cancellable) {
            if (((Cancellable) event).isCancelled() && !isIgnoreCancelled()) {
                return;
            }
        }

        consumer.accept(event, this);
    }

    @Getter
    @RequiredArgsConstructor
    @SuppressWarnings("unused")
    public static class Builder<T extends GeyserEvent> {
        private final EventManager manager;
        private final Class<T> cls;
        private final BiConsumer<T, EventHandler<T>> consumer;

        private int priority = PRIORITY.NORMAL;
        private boolean ignoreCancelled = true;

        public Builder<T> priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder<T> ignoreCancelled(boolean ignoreCancelled) {
            this.ignoreCancelled = ignoreCancelled;
            return this;
        }

        public LambdaEventHandler<T> build() {
            LambdaEventHandler<T> handler = new LambdaEventHandler<>(manager, cls, consumer, priority, ignoreCancelled);
            manager.register(handler);
            return handler;
        }
    }
}
