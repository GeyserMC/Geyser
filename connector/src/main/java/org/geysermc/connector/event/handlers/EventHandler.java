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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.connector.event.EventManager;
import org.geysermc.connector.event.GeyserEvent;

import java.util.Comparator;

@Getter
@AllArgsConstructor
public abstract class EventHandler<T extends GeyserEvent> implements Comparable<EventHandler<T>>, Comparator<EventHandler<T>> {
    private final EventManager manager;
    private final Class<T> eventClass;

    /**
     * Execute a handler for an event
     *
     * @param event Event passed to handler
     */
    public abstract void execute(T event) throws EventHandlerException;

    /**
     * Unregister ourself
     */
    public void unregister() {
        manager.unregister(this);
    }

    /**
     * Return our priority
     */
    public abstract int getPriority();

    @Override
    public int compareTo(EventHandler<T> other) {
        return getPriority() - other.getPriority();
    }

    @Override
    public int compare(EventHandler<T> left, EventHandler<T> right) {
        return left.getPriority() - right.getPriority();
    }


    public static class EventHandlerException extends Exception {
        public EventHandlerException(String message, Throwable ex) {
            super(message, ex);
        }

        public EventHandlerException(String message) {
            super(message);
        }

        public EventHandlerException(Throwable ex) {
            super(ex);
        }
    }

    public static class PRIORITY {
        public final static int LOWEST = 10;
        public final static int LOW = 4;
        public final static int NORMAL = 50;
        public final static int HIGH = 60;
        public final static int HIGHEST = 90;
    }
}
