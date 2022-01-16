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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.kyori.event.EventSubscriber;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.event.Event;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventSubscription;
import org.geysermc.geyser.api.event.Subscribe;
import org.geysermc.geyser.api.extension.Extension;

import java.util.function.Consumer;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class GeyserEventSubscription<T extends Event> implements EventSubscription<T>, EventSubscriber<T> {
    private final EventBus eventBus;
    private final Class<T> eventClass;
    private final Consumer<? super T> eventConsumer;
    private final Extension owner;
    private final Subscribe.Priority priority;
    @Getter(AccessLevel.NONE) private boolean active;

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public void unsubscribe() {
        if (!this.active) {
            return;
        }

        this.active = false;
        this.eventBus.unsubscribe(this);
    }

    @Override
    public void invoke(@NonNull T event) throws Throwable {
        try {
            this.eventConsumer.accept(event);
        } catch (Throwable ex) {
            this.owner.logger().warning("Unable to fire event " + event.getClass().getSimpleName() + " with subscription " + this.eventConsumer.getClass().getSimpleName());
            ex.printStackTrace();
        }
    }

    @Override
    public int postOrder() {
        return this.priority.postOrder();
    }
}
