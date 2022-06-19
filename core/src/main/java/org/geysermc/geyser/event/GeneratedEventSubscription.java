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

import lombok.Getter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.Event;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventListener;
import org.geysermc.geyser.api.event.Subscribe;
import org.geysermc.geyser.api.extension.Extension;

import java.util.function.BiConsumer;

@Getter
@Accessors(fluent = true)
public class GeneratedEventSubscription<T extends Event> extends AbstractEventSubscription<T> {
    private final Object eventHolder;
    private final BiConsumer<Object, ? super T> eventConsumer;

    public GeneratedEventSubscription(EventBus eventBus, Class<T> eventClass, EventListener owner, Subscribe.PostOrder order, Object eventHolder, BiConsumer<Object, ? super T> eventConsumer) {
        super(eventBus, eventClass, owner, order);

        this.eventHolder = eventHolder;
        this.eventConsumer = eventConsumer;
    }

    @Override
    public void invoke(@NonNull T event) throws Throwable {
        try {
            this.eventConsumer.accept(this.eventHolder, event);
        } catch (Throwable ex) {
            String message = "Unable to fire event " + event.getClass().getSimpleName() + " with subscription " + this.eventConsumer.getClass().getSimpleName();

            if (this.owner instanceof Extension extension) {
                extension.logger().warning(message);
            } else {
                GeyserImpl.getInstance().getLogger().warning(message);
            }

            ex.printStackTrace();
        }
    }
}
