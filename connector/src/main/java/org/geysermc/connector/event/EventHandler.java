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

import jdk.nashorn.internal.ir.annotations.Immutable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.connector.event.events.GeyserEvent;

import java.util.Comparator;

@Getter
@AllArgsConstructor
public class EventHandler<T extends GeyserEvent> implements Comparator<EventHandler<T>>, Comparable<EventHandler<T>> {
    public final Class<? extends T> eventClass;
    public final Executor<T> executor;
    public final int priority;
    public final boolean ignoreCancelled;

    public void execute(EventContext ctx, T event) {
        executor.run(ctx, event);
    }

    @Override
    public int compare(EventHandler<T> left, EventHandler<T> right) {
        return left.getPriority() - right.getPriority();
    }

    @Override
    public int compareTo(EventHandler<T> other) {
        return getPriority() - other.getPriority();
    }

    public interface Executor<T extends GeyserEvent> {
        void run(EventContext ctx, T event);
    }

    @Immutable
    public static final class PRIORITY {
        public final static int LOWEST = 10;
        public final static int LOW = 4;
        public final static int NORMAL = 50;
        public final static int HIGH = 60;
        public final static int HIGHEST = 90;
    }
}
