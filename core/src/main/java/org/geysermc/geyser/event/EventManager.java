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

package org.geysermc.geyser.event;

import lombok.Getter;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.event.annotations.GeyserEventHandler;
import org.geysermc.geyser.event.handlers.EventHandler;
import org.geysermc.geyser.event.handlers.LambdaEventHandler;
import org.geysermc.geyser.event.handlers.MethodEventHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@Getter
public class EventManager {
    @Getter
    private static EventManager instance;

    private final Map<Class<? extends GeyserEvent>, PriorityBlockingQueue<EventHandler<?>>> eventHandlers = new ConcurrentHashMap<>();
    private final Map<Object, ArrayList<EventHandler<?>>> classEventHandlers = new HashMap<>();

    public EventManager(GeyserImpl connector) {
        instance = this;
    }

    /**
     * Trigger a new event.
     *
     * All registered EventHandlers will be executed as long as they have the appropriate filter class (or none)
     * @param event Event being triggered
     * @return TriggerResult Result of the trigger
     */
    public <T extends GeyserEvent> EventResult<T> triggerEvent(T event) {
        if (event != null) {
            if (eventHandlers.containsKey(event.getClass())) {
                for (EventHandler<?> handler : eventHandlers.get(event.getClass())) {
                    try {
                        //noinspection unchecked
                        ((EventHandler<T>) handler).execute(event);
                    } catch (org.geysermc.geyser.event.handlers.EventHandler.EventHandlerException e) {
                        GeyserImpl.getInstance().getLogger().error(e.getMessage(), e);
                    }
                }
            }
        }
        return new EventResult<>(this, event);
    }

    /**
     * Create a new EventHandler using a Lambda
     *
     * @param cls event class
     * @param consumer what to execute, passed an event
     * @return an EventHandler
     */
    public <T extends GeyserEvent> LambdaEventHandler<T> on(Class<T> cls, Consumer<T> consumer) {
        return on(cls, (event, handler) -> consumer.accept(event));
    }

    public <T extends GeyserEvent> LambdaEventHandler<T> on(Class<T> cls, BiConsumer<T, EventHandler<T>> consumer) {
        return new LambdaEventHandler<>(this, cls, consumer);
    }

    /**
     * Register an EventHandler
     *
     * @param handler EventHandler to register
     */
    public <T extends GeyserEvent> void register(EventHandler<T> handler) {
        if (!eventHandlers.containsKey(handler.getEventClass())) {
            eventHandlers.put(handler.getEventClass(), new PriorityBlockingQueue<>());
        }
        eventHandlers.get(handler.getEventClass()).add(handler);
    }

    /**
     * Unregister an EventHandler
     *
     * @param handler EventHandler to unregister
     */
    public <T extends GeyserEvent> void unregister(EventHandler<T> handler) {
        if (eventHandlers.containsKey(handler.getEventClass())) {
            eventHandlers.get(handler.getEventClass()).remove(handler);
        }
    }

    /**
     * Register all Events contained in an instantiated class.
     *
     * The methods must be annotated by @GeyserEventHandler
     * @param obj The class object to look for methods annotated by @GeyserEventHandlder
     */
    public void registerEvents(Object obj) {
        List<EventHandler<?>> handlers = new ArrayList<>();
        for (Method method : obj.getClass().getMethods()) {
            // Check that the method is annotated with @Event
            if (method.getAnnotation(GeyserEventHandler.class) == null) {
                continue;
            }

            // Make sure it only has a single Event parameter
            if (method.getParameterCount() != 1 || !GeyserEvent.class.isAssignableFrom(method.getParameters()[0].getType())) {
                GeyserImpl.getInstance().getLogger().error("Cannot register EventHandler as its only parameter must be an Event: " + obj.getClass().getSimpleName() + "#" + method.getName());
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
}
