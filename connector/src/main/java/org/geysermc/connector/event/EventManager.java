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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

@Getter
@AllArgsConstructor
public class EventManager {
    private final Map<Class<? extends GeyserEvent>, PriorityQueue<EventHandler<? extends GeyserEvent>>> eventHandlers = new HashMap<>();
    private final Map<Object, ArrayList<EventHandler<?>>> classEventHandlers = new HashMap<>();

    private final GeyserConnector connector;

    /**
     * Trigger a new event
     *
     * This will be executed with all registered handlers in order of priority
     */
    public EventResult triggerEvent(GeyserEvent event) {
        if (eventHandlers.containsKey(event.getClass())) {
            for (EventHandler<? extends GeyserEvent> handler : eventHandlers.get(event.getClass())) {
                Context ctx = new Context(this, handler);

                //noinspection unchecked
                ((EventHandler<GeyserEvent>) handler).execute(ctx, event);
            }
        }
        return new Result(this, event);
    }

    /**
     * Trigger a new cancellable event
     *
     * This will be executed with all registered handlers in order of priority ignoring those who
     * do not wish to process events that are cancelled
     */
    public EventResult triggerEvent(CancellableGeyserEvent event) {
        if (eventHandlers.containsKey(event.getClass())) {
            boolean cancelled = event.isCancelled();
            for (EventHandler<? extends GeyserEvent> handler : eventHandlers.get(event.getClass())) {
                if (!cancelled || !handler.isIgnoreCancelled()) {
                    Context ctx = new Context(this, handler);

                    //noinspection unchecked
                    ((EventHandler<GeyserEvent>) handler).execute(ctx, event);

                    cancelled = event.isCancelled();
                }
            }
        }
        return new Result(this, event);
    }

    /**
     * Create a new EventHandler using an Executor
     */
    public <T extends GeyserEvent> EventRegisterResult on(Class<? extends T> cls, EventHandler.Executor<T> executor, int priority, boolean ignoreCancelled) {
        EventHandler<T> handler = new EventHandler<>(cls, executor, priority, ignoreCancelled);
        register(handler);
        return new RegisterResult(this, handler);
    }

    public <T extends GeyserEvent> EventRegisterResult on(Class<? extends T> cls, EventHandler.Executor<T> executor) {
       return on(cls, executor, EventHandler.PRIORITY.NORMAL, true);
    }

    public <T extends GeyserEvent> EventRegisterResult on(Class<? extends T> cls, EventHandler.Executor<T> executor, boolean ignoreCancelled) {
        return on(cls, executor, EventHandler.PRIORITY.NORMAL, ignoreCancelled);
    }

    public <T extends GeyserEvent> EventRegisterResult on(Class<? extends T> cls, EventHandler.Executor<T> executor, int priority) {
        return on(cls, executor, priority, true);
    }

    /**
     * Register an EventHandler
     */
    public void register(EventHandler<?> handler) {
        if (!eventHandlers.containsKey(handler.getEventClass())) {
            eventHandlers.put(handler.getEventClass(), new PriorityQueue<>());
        }
        eventHandlers.get(handler.getEventClass()).add(handler);
    }

    /**
     * Unregister an EventHandler
     */
    public void unregister(EventHandler<?> handler) {
        if (eventHandlers.containsKey(handler.getEventClass())) {
            eventHandlers.get(handler.getEventClass()).remove(handler);
        }
    }

    /**
     * Register all Events contained in an instantiated class. The methods must be annotated by @Event
     */
    public EventHandler<?>[] registerEvents(Object obj) {
        List<EventHandler<?>> handlers = new ArrayList<>();
        for (Method method : obj.getClass().getMethods()) {
            Event eventAnnotation = method.getAnnotation(Event.class);

            // Check that the method is annotated with @Event
            if (eventAnnotation == null) {
                continue;
            }

            // Make sure it only has a single Event parameter
            if (method.getParameterCount() != 2 || !GeyserEvent.class.isAssignableFrom(method.getParameters()[1].getType())) {
                continue;
            }

            //noinspection unchecked
            EventRegisterResult result = on((Class<? extends GeyserEvent>)method.getParameters()[1].getType(), (ctx, event) -> {
                        try {
                            method.invoke(obj, ctx, event);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }, eventAnnotation.priority(), eventAnnotation.ignoreCancelled());

            if (!classEventHandlers.containsKey(obj.getClass())) {
                classEventHandlers.put(obj, new ArrayList<>());
            }

            classEventHandlers.get(obj).add(result.getHandler());
            handlers.add(result.getHandler());
        }

        return handlers.toArray(new EventHandler[0]);
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
     * Provides a chainable result to event registration
     */
    @Getter
    @AllArgsConstructor
    public static class RegisterResult implements EventRegisterResult {
        private final EventManager manager;
        private final EventHandler<?> handler;

        /**
         * Execute the runnable after a delay Normally used to cancel the registration in case no event is triggered
         */
        public EventRegisterResult onDelay(Runnable runnable, long delay, TimeUnit unit) {
            manager.getConnector().getGeneralThreadPool().schedule(() -> runnable.run(new Context(manager, handler)), delay, unit);
            return this;
        }
    }

    @AllArgsConstructor
    public static class Context implements EventContext {
        private final EventManager manager;
        private final EventHandler<?> handler;

        /**
         * Unregister an EventHandler from within an Event execution
         */
        @Override
        public void unregister() {
            manager.unregister(handler);
        }
    }

    /**
     * Provides a chainable result for the triggerinig of an Event
     */
    @Getter
    @AllArgsConstructor
    public static class Result implements EventResult {
        private final EventManager manager;
        private final GeyserEvent event;

        /**
         * Returns true if the event was cancelled
         * @return boolean true if cancelled
         */
        public boolean isCancelled() {
            return event instanceof CancellableGeyserEvent && ((CancellableGeyserEvent) event).isCancelled();
        }

        public EventResult ifNotCancelled(Runnable runnable) {
            if (!isCancelled()) {
                runnable.run(this);
            }
            return this;
        }

        public EventResult ifCancelled(Runnable runnable) {
            if (isCancelled()) {
                runnable.run(this);
            }
            return this;
        }

        public EventResult orElse(Runnable runnable) {
            runnable.run(this);
            return this;
        }
    }
}
