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
import org.geysermc.connector.event.Cancellable;
import org.geysermc.connector.event.EventManager;
import org.geysermc.connector.event.annotations.GeyserEventHandler;
import org.geysermc.connector.event.GeyserEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Provides an event handler for an annotated method
 */
@Getter
public class MethodEventHandler<T extends GeyserEvent> extends EventHandler<T> {
    private final Object handlerClass;
    private final Method method;
    private final int priority;
    private final boolean ignoreCancelled;

    @SuppressWarnings("unchecked")
    public MethodEventHandler(EventManager manager, Object handlerClass, Method method) {
        super(manager, (Class<T>) method.getParameters()[0].getType());

        GeyserEventHandler annotation = method.getAnnotation(GeyserEventHandler.class);
        this.handlerClass = handlerClass;
        this.method = method;
        this.priority = annotation.priority();
        this.ignoreCancelled = annotation.ignoreCancelled();
    }

    @Override
    public void execute(T event) throws EventHandlerException {
        if (event instanceof Cancellable) {
            if (((Cancellable) event).isCancelled() && !isIgnoreCancelled()) {
                return;
            }
        }

        try {
            method.invoke(handlerClass, event);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new EventHandlerException("Unable to execute Event Handler", e);
        }
    }
}
