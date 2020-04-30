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

package org.geysermc.connector.network.translators.sound;

import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry that holds {@link SoundInteractionHandler}s.
 */
public class SoundHandlerRegistry {

    static final Map<SoundHandler, SoundInteractionHandler> INTERACTION_HANDLERS = new HashMap<>();

    static {
        Reflections ref = new Reflections("org.geysermc.connector.network.translators.sound");
        for (Class<?> clazz : ref.getTypesAnnotatedWith(SoundHandler.class)) {
            try {
                SoundInteractionHandler interactionHandler = (SoundInteractionHandler) clazz.newInstance();
                SoundHandler annotation = clazz.getAnnotation(SoundHandler.class);
                INTERACTION_HANDLERS.put(annotation, interactionHandler);
            } catch (InstantiationException | IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
    }

    private SoundHandlerRegistry() {
    }

    public static void init() {
        // no-op
    }

    /**
     * Returns a map of the interaction handlers
     *
     * @return a map of the interaction handlers
     */
    public static Map<SoundHandler, SoundInteractionHandler> getInteractionHandlers() {
        return INTERACTION_HANDLERS;
    }
}
