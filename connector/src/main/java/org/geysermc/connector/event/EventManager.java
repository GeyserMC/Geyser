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
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.event.annotations.Event;
import org.geysermc.connector.event.events.CancellableGeyserEvent;
import org.geysermc.connector.event.events.GeyserEvent;
import org.geysermc.connector.event.handlers.EventHandler;
import org.geysermc.connector.event.handlers.LambdaEventHandler;
import org.geysermc.connector.event.handlers.MethodEventHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

@SuppressWarnings("unused")
@Getter
public class EventManager {
    @Getter
    private static EventManager instance;
    private final Map<Class<? extends GeyserEvent>, PriorityBlockingQueue<EventHandler<? extends GeyserEvent>>> eventHandlers = new ConcurrentHashMap<>();
    private final Map<Object, ArrayList<EventHandler<?>>> classEventHandlers = new HashMap<>();

    private final GeyserConnector connector;

    public EventManager(GeyserConnector connector) {
        instance = this;
        this.connector = connector;
    }

    /**
     * Trigger a new event.
     *
     * All registered EventHandlers will be executed as long as they have the appropriate filter class (or none)
     * @param event Event being triggered
     * @param filter Filter Class to be tested against
     * @return TriggerResult Result of the trigger
     */
    public <T extends GeyserEvent> TriggerResult<T> triggerEvent(T event, Class<?> filter) {
        if (eventHandlers.containsKey(event.getClass())) {

            for (EventHandler<?> handler : eventHandlers.get(event.getClass())) {
                if (handler.hasFilter(filter)) {
                    try {
                        //noinspection unchecked
                        ((EventHandler<T>) handler).execute(event);
                    } catch (EventHandler.EventHandlerException e) {
                        connector.getLogger().error(e.getMessage(), e);
                    }
                }
            }
        }
        return new TriggerResult<>(this, event);
    }

    public <T extends GeyserEvent> TriggerResult<T> triggerEvent(T event) {
        return triggerEvent(event, null);
    }

    /**
     * Create a new EventHandler using a Lambda
     */
    public <T extends GeyserEvent> LambdaEventHandler.Builder<T> on(Class<T> cls, LambdaEventHandler.Runnable<T> runnable) {
        return new LambdaEventHandler.Builder<>(this, cls, runnable);
    }

    /**
     * Register an EventHandler
     */
    public <T extends GeyserEvent> void register(EventHandler<T> handler) {
        if (!eventHandlers.containsKey(handler.getEventClass())) {
            eventHandlers.put(handler.getEventClass(), new PriorityBlockingQueue<>());
        }
        eventHandlers.get(handler.getEventClass()).add(handler);
    }

    /**
     * Unregister an EventHandler
     */
    public <T extends GeyserEvent> void unregister(EventHandler<T> handler) {
        if (eventHandlers.containsKey(handler.getEventClass())) {
            eventHandlers.get(handler.getEventClass()).remove(handler);
        }
    }

    /**
     * Register all Events contained in an instantiated class. The methods must be annotated by @Event
     */
    public void registerEvents(Object obj) {
        List<EventHandler<?>> handlers = new ArrayList<>();
        for (Method method : obj.getClass().getMethods()) {
            // Check that the method is annotated with @Event
            if (method.getAnnotation(Event.class) == null) {
                continue;
            }

            // Make sure it only has a single Event parameter
            if (method.getParameterCount() != 1 || !GeyserEvent.class.isAssignableFrom(method.getParameters()[0].getType())) {
                connector.getLogger().error("Cannot register EventHander as its only parameter must be an Event: " + obj.getClass().getSimpleName() + "#" + method.getName());
                continue;
            }

            EventHandler<?> handler = new MethodEventHandler<>(this, obj, method);
            register(handler);
            handlers.add(handler);
        }

        if (!classEventHandlers.containsKey(obj.getClass())) {
            classEventHandlers.put(obj.getClass(), new ArrayList<>());
        }
        classEventHandlers.get(obj.getClass()).addAll(handlers);
    }

    /**
     * Unregister all events in class
     */
    public void unregisterEvents(Object obj) {
        if (!classEventHandlers.containsKey(obj)) {
            return;
        }

        for (EventHandler<?> handler : classEventHandlers.get(obj)) {
            unregister(handler);
        }

        classEventHandlers.remove(obj);
    }

    /**
     * Provides a chainable result for the triggerinig of an Event
     */
    @SuppressWarnings({"UnusedReturnValue", "unused"})
    @Getter
    @AllArgsConstructor
    public static class TriggerResult<T> {
        private final EventManager manager;
        private final T event;
        private final boolean valid;

        public TriggerResult(EventManager manager, T event) {
            this(manager, event, true);
        }

        /**
         * Returns true if the event was cancelled
         * @return boolean true if cancelled
         */
        public boolean isCancelled() {
            return event instanceof CancellableGeyserEvent && ((CancellableGeyserEvent) event).isCancelled();
        }

        public TriggerResult<T> onNotCancelled(Runnable<T> runnable) {
            if (!isCancelled()) {
                runnable.run(this);
                return new TriggerResult<>(manager, event, false);
            }
            return this;
        }

        public TriggerResult<T> onCancelled(Runnable<T> runnable) {
            if (isCancelled()) {
                runnable.run(this);
                return new TriggerResult<>(manager, event, false);
            }
            return this;
        }

        public TriggerResult<T> orElse(Runnable<T> runnable) {
            if (valid) {
                runnable.run(this);
                return new TriggerResult<>(manager, event, false);
            }
            return this;
        }

        public interface Runnable<T> {
            void run(TriggerResult<T> result);
        }
    }
}
