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
import org.geysermc.connector.event.handlers.LambdaEventHandler;
import org.geysermc.connector.plugin.GeyserPlugin;

/**
 * Provides a lambda event handler for a plugin.
 */
@Getter
public class PluginLambdaEventHandler<T extends GeyserEvent> extends LambdaEventHandler<T> {
    private final GeyserPlugin plugin;

    public PluginLambdaEventHandler(GeyserPlugin plugin, Class<T> cls, Runnable<T> runnable, int priority, boolean ignoreCancelled, Class<?>[] filter) {
        super(plugin.getEventManager(), cls, runnable, priority, ignoreCancelled, filter);

        this.plugin = plugin;
    }

    @Override
    public void unregister() {
        plugin.unregister(this);
        super.unregister();
    }

    @Getter
    public static class Builder<T extends GeyserEvent> extends LambdaEventHandler.Builder<T> {
        private final GeyserPlugin plugin;

        public Builder(GeyserPlugin plugin, Class<T> cls, Runnable<T> runnable) {
            super(plugin.getEventManager(), cls, runnable);

            this.plugin = plugin;
        }

        @Override
        public LambdaEventHandler<T> build() {
            LambdaEventHandler<T> handler = new PluginLambdaEventHandler<>(plugin, getCls(), getRunnable(), getPriority(), isIgnoreCancelled(), getFilter());
            plugin.register(handler);
            return handler;
        }
    }
}
