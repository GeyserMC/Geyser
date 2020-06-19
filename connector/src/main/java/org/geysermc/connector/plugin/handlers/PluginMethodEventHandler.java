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

package org.geysermc.connector.plugin.handlers;

import lombok.Getter;
import org.geysermc.connector.event.events.GeyserEvent;
import org.geysermc.connector.event.handlers.MethodEventHandler;
import org.geysermc.connector.plugin.GeyserPlugin;

import java.lang.reflect.Method;

/**
 * Provides a method event handler for a plugin.
 */
@Getter
public class PluginMethodEventHandler<T extends GeyserEvent> extends MethodEventHandler<T> {
    private final GeyserPlugin plugin;

    public PluginMethodEventHandler(GeyserPlugin plugin, Object handlerClass, Method method) {
        super(plugin.getEventManager(), handlerClass, method);

        this.plugin = plugin;
    }

    @Override
    public void unregister() {
        plugin.unregister(this);
        super.unregister();
    }
}
