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

package org.geysermc.connector.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Provides a chainable result for the triggering of an Event
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
@Getter
@AllArgsConstructor
public class EventResult<T extends GeyserEvent> {
    private final EventManager manager;
    private final T event;
    private final boolean valid;

    public EventResult(EventManager manager, T event) {
        this(manager, event, true);
    }

    /**
     * Returns true if the event was cancelled
     * @return boolean true if cancelled
     */
    public boolean isCancelled() {
        return event instanceof Cancellable && ((Cancellable) event).isCancelled();
    }

    public EventResult<T> onNotCancelled(Runnable<T> runnable) {
        if (!isCancelled()) {
            runnable.run(this);
            return new EventResult<>(manager, event, false);
        }
        return this;
    }

    public EventResult<T> onCancelled(Runnable<T> runnable) {
        if (isCancelled()) {
            runnable.run(this);
            return new EventResult<>(manager, event, false);
        }
        return this;
    }

    public EventResult<T> orElse(Runnable<T> runnable) {
        if (valid) {
            runnable.run(this);
            return new EventResult<>(manager, event, false);
        }
        return this;
    }

    public interface Runnable<T extends GeyserEvent> {
        void run(EventResult<T> result);
    }
}