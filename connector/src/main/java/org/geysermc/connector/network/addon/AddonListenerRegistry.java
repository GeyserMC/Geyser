/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.addon;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry containing all the {@link AddonListener}s listening
 * for plugin messages.
 */
public class AddonListenerRegistry {

    private static final Map<String, AddonListener<?>> LISTENERS = new HashMap<>();

    private AddonListenerRegistry() {
    }

    public static void init() {
        registerListener(FormAddonListener.get());
    }

    /**
     * Returns all the {@link AddonListener}s listening for
     * plugin messages.
     *
     * Key: the subchannel the listener is listening on
     * Value: the {@link AddonListener}
     *
     * @return all the {@link AddonListener}s listening for plugin messages.
     */
    public static Map<String, AddonListener<?>> getListeners() {
        return LISTENERS;
    }

    private static void registerListener(AddonListener<?> listener) {
        LISTENERS.put(listener.getSubChannel(), listener);
    }
}
